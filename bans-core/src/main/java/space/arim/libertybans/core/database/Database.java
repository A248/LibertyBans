/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.DelayCalculators;
import space.arim.omnibus.util.concurrent.ScheduledWork;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentDatabase;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.AddressVictim.NetworkAddress;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.core.LibertyBansCore;

import space.arim.jdbcaesar.JdbCaesar;
import space.arim.jdbcaesar.adapter.EnumNameAdapter;
import space.arim.jdbcaesar.builder.JdbCaesarBuilder;
import space.arim.jdbcaesar.transact.IsolationLevel;

public class Database implements PunishmentDatabase {

	private final LibertyBansCore core;
	private final Vendor vendor;
	private final ExecutorService executor;
	private final JdbCaesar jdbCaesar;
	
	private ScheduledWork<?> hyperSqlRefreshTask;
	
	static final int REVISION_MAJOR = 1;
	static final int REVISION_MINOR = 0;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private static final boolean TRACE_FOREIGN_CONNECTIONS;
	
	static {
		boolean traceForeign = false;
		try {
			traceForeign = Boolean.getBoolean("space.arim.libertybans.traceForeignConnections");
		} catch (SecurityException ignored) {}
		TRACE_FOREIGN_CONNECTIONS = traceForeign;
	}
	
	Database(LibertyBansCore core, Vendor vendor, HikariConfig hikariConf, int poolSize) {
		this.core = core;
		this.vendor = vendor;

		executor = Executors.newFixedThreadPool(poolSize, new IOUtils.ThreadFactoryImpl("LibertyBans-Database-"));
		HikariWrapper connectionSource = new HikariWrapper(new HikariDataSource(hikariConf));

		JdbCaesarBuilder jdbCaesarBuilder = new JdbCaesarBuilder()
				.connectionSource(connectionSource)
				.exceptionHandler((ex) -> {
					logger.error("Error while executing a database query", ex);
				})
				.defaultIsolation(IsolationLevel.REPEATABLE_READ)
				.rewrapExceptions(true)
				.addAdapters(
						new EnumNameAdapter<>(PunishmentType.class),
						new JdbCaesarHelper.VictimAdapter(),
						new EnumNameAdapter<>(Victim.VictimType.class),
						new JdbCaesarHelper.OperatorAdapter(),
						new JdbCaesarHelper.ScopeAdapter(core.getScopeManager()),
						new JdbCaesarHelper.UUIDBytesAdapter(),
						new JdbCaesarHelper.InetAddressAdapter());
		if (vendor.noUnsignedNumerics()) {
			jdbCaesarBuilder.addAdapter(
					new JdbCaesarHelper.UnsigningTimestampAdapter());
		}
		jdbCaesar = jdbCaesarBuilder.build();
	}
	
	void startRefreshTaskIfNecessary() {
		if (getVendor() != Vendor.MARIADB) {
			synchronized (this) {
				hyperSqlRefreshTask = core.getResources().getEnhancedExecutor().scheduleRepeating(
						new RefreshTaskRunnable(core.getDatabaseManager(), this),
						Duration.ofHours(1L), DelayCalculators.fixedDelay());
			}
		}
	}
	
	void cancelRefreshTaskIfNecessary() {
		if (getVendor() != Vendor.MARIADB) {
			synchronized (this) {
				hyperSqlRefreshTask.cancel();
			}
		}
	}
	
	public Vendor getVendor() {
		return vendor;
	}
	
	void close() {
		try {
			jdbCaesar.getConnectionSource().close();
		} catch (SQLException ex) {
			throw new AssertionError(ex);
		}
		executor.shutdown();
	}
	
	void closeCompletely() {
		if (getVendor() == Vendor.HSQLDB) {
			jdbCaesar().query("SHUTDOWN").voidResult().execute();
		}
		close();
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("Foreign caller acquiring connection on thread  {}", Thread.currentThread());
			if (TRACE_FOREIGN_CONNECTIONS) {
				logger.trace("Call trace as requested by space.arim.libertybans.traceForeignConnections=true", new Exception());
			}
		}
		return jdbCaesar.getConnectionSource().getConnection();
	}
	
	public JdbCaesar jdbCaesar() {
		return jdbCaesar;
	}

	@Override
	public int getMajorRevision() {
		return REVISION_MAJOR;
	}

	@Override
	public int getMinorRevision() {
		return REVISION_MINOR;
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}
	
	/*
	 * 
	 * Helper thread pool methods
	 * 
	 */
	
	public CentralisedFuture<?> executeAsync(Runnable command) {
		return core.getFuturesFactory().runAsync(command, executor);
	}
	
	public <T> CentralisedFuture<T> selectAsync(Supplier<T> supplier) {
		return core.getFuturesFactory().supplyAsync(supplier, executor);
	}
	
	/*
	 * 
	 * Helper retrieval methods
	 * 
	 */
	
	public PunishmentType getTypeFromResult(ResultSet resultSet) throws SQLException {
		return PunishmentType.valueOf(resultSet.getString("type"));
	}
	
	public Victim getVictimFromResult(ResultSet resultSet) throws SQLException {
		VictimType vType = VictimType.valueOf(resultSet.getString("victim_type"));
		byte[] bytes = resultSet.getBytes("victim");
		switch (vType) {
		case PLAYER:
			return PlayerVictim.of(UUIDUtil.fromByteArray(bytes));
		case ADDRESS:
			return AddressVictim.of(new NetworkAddress(bytes));
		default:
			throw new IllegalStateException("Unknown victim type " + vType);
		}
	}

	public Operator getOperatorFromResult(ResultSet resultSet) throws SQLException {
		return JdbCaesarHelper.getOperatorFromResult(resultSet);
	}
	
	public String getReasonFromResult(ResultSet resultSet) throws SQLException {
		return resultSet.getString("reason");
	}

	public Scope getScopeFromResult(ResultSet resultSet) throws SQLException {
		String server = resultSet.getString("scope");
		if (server != null) {
			return core.getScopeManager().specificScope(server);
		}
		return core.getScopeManager().globalScope();
	}
	
	public long getStartFromResult(ResultSet resultSet) throws SQLException {
		long directValue = resultSet.getLong("start");
		if (getVendor().noUnsignedNumerics()) {
			directValue -= Integer.MIN_VALUE;
		}
		return directValue;
	}
	
	public long getEndFromResult(ResultSet resultSet) throws SQLException {
		long directValue = resultSet.getLong("end");
		if (getVendor().noUnsignedNumerics()) {
			directValue -= Integer.MIN_VALUE;
		}
		return directValue;
	}

}
