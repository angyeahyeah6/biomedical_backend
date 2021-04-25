package ken.util;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by lbj23k on 2017/3/8.
 */
public class JDBCHelper {
    private static QueryRunner runner = new QueryRunner();

    public static List<Map<String, Object>> query(String schema, String sql, Object... params) {
        Connection conn = null;
        List<Map<String, Object>> result = null;
        try {
            conn = DbConnector.getInstance().getConnectionByType(schema);
            result = runner.query(conn, sql, new MapListHandler(), params);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
        return result;
    }

    public static List<Map<String, Object>> query(String schema, String sql) {
        Connection conn = null;
        List<Map<String, Object>> result = null;
        try {
            conn = DbConnector.getInstance().getConnectionByType(schema);
            result = runner.query(conn, sql, new MapListHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
        return result;
    }

    public static void updateSingle(String schema, String sql, Object... params) {
        Connection conn = null;
        int result = 0;
        try {
            conn = DbConnector.getInstance().getConnectionByType(schema);
            result = runner.update(conn, sql, params);
            if (result == 0) {
                throw new SQLException("update fails");
            }
        } catch (SQLException e) {
            System.out.println(params[0]);
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    public static void updateBatch(String schema, String sql, Object[][] params) {
        Connection conn = null;
        try {
            conn = DbConnector.getInstance().getConnectionByType(schema);
            conn.setAutoCommit(false);
            runner.batch(conn, sql, params);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }
}
