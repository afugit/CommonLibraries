/**
 * Provides simple interface for connecting to a MySQL database and executing
 * common queries.
 *
 * Ported over from PHP project vexObj.
 *
 * @author Anthony M. Fugit
 * @version 0.1.20171010
 *
 * @backlog Review lines 141-149
 * @backlog Review lines 170-178
 */

package com.vonexus;

import java.sql.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

public class MySQL {
    /**
     * Start logger
     */

    private static final Logger LOGGER = Logger.getLogger(MySQL.class.getName());

    /**
     * Class Variables to store configuration and debugging information in.
     */

    private String sqlServer = "";
    private String sqlDatabase = "";
    private String sqlUsername = "";
    private String sqlPassword = "";
    private String sqlQuery = "";
    private int sqlPort = 3306;
    private boolean sqlReconnect = true;
    private boolean sqlSSL = false;
    private String sqlConnection;
    private Connection sqlObj;

    /**
     * Constructors used to create database connection since it is the central
     * component the rest of the class depends on.
     *
     * @param sqlServer    ip or dns name to mysql
     * @param sqlDatabase  database schema to use
     * @param sqlUsername  username for database
     * @param sqlPassword  password for database
     * @param sqlPort      (optional) default 3306
     * @param sqlReconnect (optional) default true
     * @param sqlSSL       (optional) default false
     */

    public MySQL(String sqlServer, String sqlDatabase, String sqlUsername, String sqlPassword) {
        this(sqlServer, sqlDatabase, sqlUsername, sqlPassword, 3306, true, false);
    }

    public MySQL(String sqlServer, String sqlDatabase, String sqlUsername, String sqlPassword, int sqlPort, boolean sqlReconnect, boolean sqlSSL) {
        this.setSqlServer(sqlServer);
        this.setSqlDatabase(sqlDatabase);
        this.setSqlUsername(sqlUsername);
        this.setSqlPassword(sqlPassword);
        this.setSqlPort(sqlPort);
        this.setSqlReconnect(sqlReconnect);
        this.setSqlSSL(sqlSSL);

        // jdbc:mysql://127.0.0.1:3306/scribo?autoReconnect=true&useSSL=false";
        this.setSqlConnection(String.format("jdbc:mysql://%s:%d/%s?autoReconnect=%s&useSSL=%s", sqlServer, sqlPort, sqlDatabase, sqlReconnect, sqlSSL));
        this.openDB();
    }

    /**
     * Accessors and Mutators for private class variables.
     *
     * @param sqlStrings used by openDB() and logger
     * @return sqlStrings used by openDB() and logger
     */

    private String getSqlServer() {
        return sqlServer;
    }
    private void setSqlServer(String sqlServer) {
        this.sqlServer = sqlServer;
    }
    private String getSqlDatabase() {
        return sqlDatabase;
    }
    private void setSqlDatabase(String sqlDatabase) {
        this.sqlDatabase = sqlDatabase;
    }
    private String getSqlUsername() {
        return sqlUsername;
    }
    private void setSqlUsername(String sqlUsername) {
        this.sqlUsername = sqlUsername;
    }
    private String getSqlPassword() {
        return sqlPassword;
    }
    private void setSqlPassword(String sqlPassword) {
        this.sqlPassword = sqlPassword;
    }
    private String getSqlConnection() {
        return sqlConnection;
    }
    private void setSqlConnection(String sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    private int getSqlPort() {
        return sqlPort;
    }
    private void setSqlPort(int sqlPort) {
        this.sqlPort = sqlPort;
    }
    private boolean getSqlReconnect() {
        return sqlReconnect;
    }

    private void setSqlReconnect(boolean sqlReconnect) {
        this.sqlReconnect = sqlReconnect;
    }

    private String getSqlQuery() {
        return sqlQuery;
    }
    private void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    private boolean getSqlSSL() {
        return sqlSSL;
    }

    private void setSqlSSL(boolean sqlSSL) {
        this.sqlSSL = sqlSSL;
    }


    /**
     * Open database with the parameters passed from constructor.
     */

    private void openDB() {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            // connect to msyql database and execute query
            Connection conn = DriverManager.getConnection(this.getSqlConnection(), this.getSqlUsername(), this.getSqlPassword());
            LOGGER.log(INFO, "Opened database successfully: " + this.getSqlServer() + " > " + this.getSqlDatabase());
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage());
        }
    }

    /**
     * Used by the various interfaces available to the user that does not
     * require a return variable - insert, update, delete queries.
     * <p>
     * TODO: Verify that creating a shadow copy of cSqlObj works
     * TODO: Verify if Class.forName() is required
     *
     * @param query properly formatted query that is ready to be executed
     */

    private void executeQuery(String query) { // used for insert/update/delete
        Connection cSqlObj = this.sqlObj; // create copy as we don't want createStatement() attached to original.

        this.setSqlQuery(query);

        try (Statement stmt = cSqlObj.createStatement()) {
            // Class.forName("com.mysql.jdbc.Driver"); may not need this, perhaps required to build cSqlObj which is done already.

            if (stmt.execute(query)) {
                LOGGER.log(INFO, "Query Ran: " + query);
            }
            stmt.close();
        } catch (Exception e) {
            LOGGER.log(SEVERE, "Query Failed: " + query);
            LOGGER.log(SEVERE, e.getMessage());
        }
    }

    /**
     * Used by the various interfaces available to the user that does
     * require a return variable - select
     * <p>
     * TODO: Verify if Class.forName() is required
     *
     * @param query properly formatted query that is ready to be executed
     * @return a list of query results
     */

    private ResultSet runQuery(String query) { // used for returning data
        Connection cSqlObj = this.sqlObj; // create copy as we don't want createStatement() attached to original.
        ResultSet rs;

        this.setSqlQuery(query);

        try (Statement stmt = cSqlObj.createStatement()) {
            // Class.forName("com.mysql.jdbc.Driver");

            rs = stmt.executeQuery(query);
            LOGGER.log(INFO, "Query Ran: " + query);
            stmt.close();
            return rs;
        } catch (Exception e) {
            rs = null;
            LOGGER.log(SEVERE, "Query Failed: " + query);
            LOGGER.log(SEVERE, e.getMessage());
        }

        return rs;
    }

    /**
     * Overloaded method that gets information from a table.
     *
     * @param table  targeted database table
     * @param where  (optional) where clause to add to query.  default = WHERE 1
     * @param fields (optional) which fields to return.  default = *
     * @return resultset with query response
     */

    public ResultSet getFromDatabase(String table) {
        if (table.isEmpty()) {
            LOGGER.log(WARNING, "Query Warning:  Missing table parameter");
        }
        ResultSet rs = this.getFromDatabase(table, "WHERE 1"); //overload
        return rs;
    }

    public ResultSet getFromDatabase(String table, String where) {
        if (table.isEmpty() || where.isEmpty()) {
            LOGGER.log(WARNING, "Query Warning:  Missing table or where parameter");
        }
        ResultSet rs = this.getFromDatabase(table, where, "*"); //overload
        return rs;
    }

    public ResultSet getFromDatabase(String table, String where, String columns) {
        String query = "";
        ResultSet rs;

        if (table.isEmpty() || where.isEmpty() || columns.isEmpty()) {
            LOGGER.log(SEVERE, "Query Error:  Missing table, query, or columns; not executed > " + query);
        }
        query = String.format("SELECT %s FROM %s WHERE %s", columns, table, where);
        rs = this.runQuery(query);

        return rs;
    }

    /**
     * Prints out friendly version of ResultSet received by getFromDatabase, ResultQuery, etc.
     *
     * @param rs ResultSet to parse through, normally through getFromDatabase() results
     */

    public void debug(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        LOGGER.log(Level.INFO, this.getSqlQuery());

        int columnsNumber = rsmd.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1)
                    LOGGER.log(Level.INFO, ", ");
                String columnValue = rs.getString(i);
                LOGGER.log(Level.INFO, columnValue + "=" + rsmd.getColumnName(i));
            }
        }
    }

    /**
     * Inserting a hashmap of data into table
     *
     * @param table targeted database table
     * @param data  map of data to insert into table.  for now this is one at a
     *              time but perhaps later will update to support 2d arrays to insert
     *              multiple re one query.
     */

    public void insertIntoTable(String table, HashMap<String, String> data) { // INSERT INTO table_name (column1, column2, column3, ... ) VALUES (value1, value2, value3, ...)
        String query = String.format("INSERT INTO %s (", table);
        String subQuery = "";

        int dataLen = data.size();
        if (dataLen == 0)
            return; // nothing to do

        data.forEach((k, v) -> {
            query.concat(String.format("%s, ", k));
            subQuery.concat(String.format("\'%s\', ", v));
        });
        query.substring(0, query.length() - 2); // trim off last comma
        subQuery.substring(0, query.length() - 2); // trim off last comma
        query.concat(String.format(") VALUES (%s)", subQuery));

        this.executeQuery(query);
    }

    /**
     * Updating data into a database
     *
     * @param table targeted database table
     * @param data  map of data to update
     * @param where which record should receive update
     */

    public void updateToDatabase(String table, HashMap<String, String> data, String where) {  // update table set col1 = val1, col2 = val2 where 1
        String query = String.format("UPDATE %s SET", table);

        int dataLen = data.size();
        if (dataLen == 0)
            return; // nothing to do

        data.forEach((k, v) -> {
            query.concat(String.format("%s = \'%s\', ", k, v));
        });
        query.substring(0, query.length() - 2); // trim off last comma

        this.executeQuery(query);
    }

    /**
     * Delete data from database using where clause  No overloading to
     * this.purgeTable() to prevent accidental deletion of data.
     *
     * @param table targeted database table
     * @param where (optional) which records should be deleted
     *
     * @see this.purgeTable();
     */

    public void deleteFromDatabase(String table, String where) {
        String query = String.format("DELETE FROM %s WHERE %s", table, where);
        this.executeQuery(query);
    }

    /**
     * Purges entire table and contents, resets AI to 1
     *
     * @param table
     */
    public void purgeTable(String table) {
        String query = String.format("DELETE FROM %s", table);
        this.executeQuery(query);
    }

    public void debug(Exception e) {
        String sqlServer = this.getSqlServer();
        String sqlDatabase = this.getSqlDatabase();
        String sqlConnection = this.getSqlConnection();
        int sqlPort = this.getSqlPort();
        boolean sqlReconnect = this.getSqlReconnect();
        boolean sqlSSL = this.getSqlSSL();
        String sqlQuery = this.getSqlQuery();


    }

}
