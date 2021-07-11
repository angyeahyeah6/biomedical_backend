package weng.util;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by lbj23k on 2017/3/8.
 */
public class JDBCHelper {
    private QueryRunner runner;

    public JDBCHelper() {
        this.runner = new QueryRunner();
    }

    public List<Map<String, Object>> query(Connection conn, String sql, Object... params) {
        List<Map<String, Object>> result = null;
        try {
            result = runner.query(conn, sql, new MapListHandler(), params);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Map<String, Object>> query(String schema, String sql, Object... params) {
        Connection conn = null;
        List<Map<String, Object>> result = null;
        try {
            DbConnector dbConnector = new DbConnector();
            conn = dbConnector.getConnectionByType(schema);
            result = runner.query(conn, sql, new MapListHandler(), params);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
        return result;
    }

    public List<Map<String, Object>> query(Connection conn, String sql) {
        List<Map<String, Object>> result = null;
        try {
            result = runner.query(conn, sql, new MapListHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Map<String, Object>> query(String schema, String sql) {
        Connection conn = null;
        List<Map<String, Object>> result = null;
        try {
            DbConnector dbConnector = new DbConnector();
            conn = dbConnector.getConnectionByType(schema);
            result = runner.query(conn, sql, new MapListHandler());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
        return result;
    }

    public void updateSingle(String schema, String sql, Object... params) {
        Connection conn = null;
        int result = 0;
        try {
            DbConnector dbConnector = new DbConnector();
            conn = dbConnector.getConnectionByType(schema);
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

    public void updateBatch(String schema, String sql, Object[][] params) {
        Connection conn = null;
        try {
            DbConnector dbConnector = new DbConnector();
            conn = dbConnector.getConnectionByType(schema);
            conn.setAutoCommit(false);
            runner.batch(conn, sql, params);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    public int insert(String schema, String sql, Object... params) {
        Connection conn = null;
        int _id = 0;
        try {
            DbConnector dbConnector = new DbConnector();
            conn = dbConnector.getConnectionByType(schema);
            _id = (runner.insert(conn, sql, new ScalarHandler<BigInteger>(), params)).intValue();
        } catch (SQLException e) {
            System.out.println(params[0]);
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
        return _id;
    }

    public int insert(Connection conn, String sql, Object... params) {
        int _id = 0;
        try {
            _id = (runner.insert(conn, sql, new ScalarHandler<BigInteger>(), params)).intValue();
        } catch (SQLException e) {
            System.out.println(params[0]);
            e.printStackTrace();
        }
        return _id;
    }
}
