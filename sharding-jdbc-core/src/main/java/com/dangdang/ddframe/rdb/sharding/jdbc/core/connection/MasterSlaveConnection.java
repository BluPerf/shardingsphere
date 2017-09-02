/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.jdbc.core.connection;

import com.dangdang.ddframe.rdb.sharding.constant.SQLType;
import com.dangdang.ddframe.rdb.sharding.hint.HintManagerHolder;
import com.dangdang.ddframe.rdb.sharding.jdbc.adapter.AbstractConnectionAdapter;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.MasterSlaveDataSource;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.statement.MasterSlavePreparedStatement;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.statement.MasterSlaveStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.SQLJudgeEngine;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.sql.SQLStatement;
import com.google.common.base.Optional;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Connection that support master-slave.
 * 
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class MasterSlaveConnection extends AbstractConnectionAdapter {
    
    private final MasterSlaveDataSource masterSlaveDataSource;
    
    private final Map<String, Connection> connectionMap = new HashMap<>();
    
    /**
     * Get database connections via SQL.
     *
     * <p>DDL will return all connections; DQL will return slave connection; DML or updated before in same thread will return master connection.</p>
     * 
     * @param sql SQL
     * @return database connections via SQL
     * @throws SQLException SQL exception
     */
    public Collection<Connection> getConnection(final String sql) throws SQLException {
        SQLStatement sqlStatement = new SQLJudgeEngine(sql).judge();
        Map<String, DataSource> dataSources = SQLType.DDL == sqlStatement.getType() ? masterSlaveDataSource.getAllDataSources() : masterSlaveDataSource.getDataSource(sqlStatement.getType()).toMap();
        Collection<Connection> result = new LinkedList<>();
        for (Entry<String, DataSource> each : dataSources.entrySet()) {
            String dataSourceName = each.getKey();
            Optional<Connection> cachedConnection = getCachedConnection(dataSourceName);
            if (cachedConnection.isPresent()) {
                result.add(cachedConnection.get());
                continue;
            }
            Connection connection = each.getValue().getConnection();
            connectionMap.put(dataSourceName, connection);
            result.add(connection);
            replayMethodsInvocation(connection);
            
        }
        return result;
    }
    
    private Optional<Connection> getCachedConnection(final String dataSourceName) {
        return Optional.fromNullable(connectionMap.get(dataSourceName));
    }
    
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return masterSlaveDataSource.getDataSource(SQLType.DML).getDataSource().getConnection().getMetaData();
    }
    
    @Override
    public Statement createStatement() throws SQLException {
        return new MasterSlaveStatement(this);
    }
    
    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        return new MasterSlaveStatement(this, resultSetType, resultSetConcurrency);
    }
    
    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        return new MasterSlaveStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        return new MasterSlavePreparedStatement(this, sql);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        return new MasterSlavePreparedStatement(this, sql, resultSetType, resultSetConcurrency);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        return new MasterSlavePreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        return new MasterSlavePreparedStatement(this, sql, autoGeneratedKeys);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        return new MasterSlavePreparedStatement(this, sql, columnIndexes);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
        return new MasterSlavePreparedStatement(this, sql, columnNames);
    }
    
    @Override
    public Collection<Connection> getCachedConnections() throws SQLException {
        return connectionMap.values();
    }
    
    @Override
    public void close() throws SQLException {
        HintManagerHolder.clear();
        MasterSlaveDataSource.resetDMLFlag();
        super.close();
    }
}
