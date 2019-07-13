package cat.nyaa.nyaacore.orm.backends;

import cat.nyaa.nyaacore.orm.ObjectFieldModifier;
import cat.nyaa.nyaacore.orm.ObjectModifier;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class SQLiteDatabase implements IConnectedDatabase {

    private final Connection dbConn;

    public SQLiteDatabase(Connection sqlConnection) {
        if (sqlConnection == null) throw new IllegalArgumentException();
        dbConn = sqlConnection;
    }

    @Override
    public void close() throws SQLException {
        dbConn.close();
    }

    @Override
    public boolean verifySchema(String tableName, Class recordClass) {
        if (tableName == null || recordClass == null) throw new IllegalArgumentException();
        if (tableName.equals(recordClass.getName())) throw new IllegalArgumentException("table name must match");
        ObjectModifier objectModifier = ObjectModifier.fromClass(recordClass);

        boolean matches = true;
        try {
            Map<String, ObjectFieldModifier> objectColumns = new HashMap<>(objectModifier.columns);

            ResultSet columnRS = dbConn.getMetaData().getColumns(null, null, tableName, "%");
            while (columnRS.next()) {
                String colName = columnRS.getString("COLUMN_NAME");
                int colType = columnRS.getInt("DATA_TYPE");
                int nullable = columnRS.getInt("NULLABLE"); // 0=NotNull 1=Nullable 2=Unknown

                ObjectFieldModifier tmp = objectColumns.remove(colName);
                if (tmp == null) {
                    Bukkit.getLogger().info(String.format("table column %s.%s not exists in class %s", tableName, colName, recordClass.getCanonicalName()));
                    matches = false;
                } else {
                    JDBCType tableColType = JDBCType.valueOf(colType);
                    if (!tmp.typeConverter.getSqlType().equals(tableColType)) {
                        Bukkit.getLogger().info(String.format("table column %s.%s type mismatch. db:%s java:%s",
                                tableName, colName, tableColType, tmp.typeConverter.getSqlType()));
                        matches = false;
                    } else if (nullable == 0 && tmp.nullable || nullable == 1 && !tmp.nullable) {
                        Bukkit.getLogger().info(String.format("table column %s.%s nullable mismatch db:%d java:%s",
                                tableName, colName, nullable, tmp.nullable));
                        matches = false;
                    }
                }
            }
            columnRS.close();

            for (String col : objectColumns.keySet()) {
                Bukkit.getLogger().info(String.format("table column %s.%s not in database", tableName, col));
                matches = false;
            }

            ResultSet pkRs = dbConn.getMetaData().getPrimaryKeys(null, null, tableName);
            if (pkRs.next()) {
                String pkColName = pkRs.getString("COLUMN_NAME");
                if (objectModifier.primaryKey == null) {
                    Bukkit.getLogger().info(String.format("table column %s.%s is primary key but java does not",
                            tableName, pkColName));
                    matches = false;
                } else if (!objectModifier.primaryKey.equals(pkColName)) {
                    Bukkit.getLogger().info(String.format("table column %s.%s is primary key but java use %s",
                            tableName, pkColName, objectModifier.primaryKey));
                    matches = false;
                } else if (pkRs.next()) {
                    pkRs.close();
                    throw new RuntimeException("multiple primary keys wtf ???");
                }
            } else if (objectModifier.primaryKey != null) {
                Bukkit.getLogger().info(String.format("table column %s.%s is not primary key but java says so",
                        tableName, objectModifier.primaryKey));
                matches = false;
            }
            pkRs.close();

            return matches;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (ResultSet rs = dbConn.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    @Override
    public <T> ITable<T> getTable(Class<T> recordClass) {
        if (recordClass == null) throw new IllegalArgumentException();
        ObjectModifier<T> om = ObjectModifier.fromClass(recordClass);

        try {
            if (tableExists(om.tableName)) {
                if (!verifySchema(om.tableName, recordClass)) {
                    throw new RuntimeException("table schema not match");
                } else {
                    return this.new SQLiteTypedTable<>(om);
                }
            } else {
                createTable(recordClass);
                return this.new SQLiteTypedTable<>(om);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String getTableCreationScheme(ObjectFieldModifier fm) {
        String ret = fm.name + " " + fm.columnDefinition;
        if (!fm.nullable) ret += " NOT NULL";
        if (fm.unique) ret += " UNIQUE";
        return ret;
    }

    private static <T> String getTableCreationSql(Class<T> recordClass) {
        ObjectModifier<T> objMod = ObjectModifier.fromClass(recordClass);
        StringJoiner colStr = new StringJoiner(",");
        for (String colName : objMod.getColNames()) {
            ObjectFieldModifier ct = objMod.columns.get(colName);
            if (ct.primary) {
                if (ct.typeConverter.getSqlType().equals(JDBCType.INTEGER) || ct.typeConverter.getSqlType().equals(JDBCType.BIGINT)) {
                    colStr.add(ct.getName() + " INTEGER PRIMARY KEY");
                } else {
                    colStr.add(getTableCreationScheme(ct) + " PRIMARY KEY");
                }
            } else {
                colStr.add(getTableCreationScheme(ct));
            }
        }
        return String.format("CREATE TABLE IF NOT EXISTS %s(%s)", objMod.tableName, colStr.toString());
    }

    private <T> void createTable(Class<T> cls) {
        if (cls == null) throw new IllegalArgumentException();
        ObjectModifier om = ObjectModifier.fromClass(cls);
        String sql = getTableCreationSql(cls);
        try (Statement smt = dbConn.createStatement()) {
            smt.executeUpdate(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(sql, ex);
        }
    }

    public class SQLiteTypedTable<T> extends BaseTypedTable<T> {
        private final ObjectModifier<T> javaObjectModifier;
        private final String tableName;

        public SQLiteTypedTable(ObjectModifier<T> javaObjectModifier) {
            this.javaObjectModifier = javaObjectModifier;
            this.tableName = javaObjectModifier.tableName;
        }

        @Override
        public String getTableName() {
            return tableName;
        }

        @Override
        public ObjectModifier<T> getJavaTypeModifier() {
            return javaObjectModifier;
        }

        @Override
        protected Connection getConnection() {
            return dbConn;
        }
    }
}
