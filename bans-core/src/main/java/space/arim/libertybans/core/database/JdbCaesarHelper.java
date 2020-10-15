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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.core.punish.Scoper;

import space.arim.jdbcaesar.adapter.DataTypeAdapter;

public class JdbCaesarHelper {
	
	private static final byte[] consoleUUIDBytes = UUIDUtil.toByteArray(new UUID(0, 0));

	private JdbCaesarHelper() {}
	
	/**
	 * Gets an operator from a result set
	 * 
	 * @param resultSet the result set
	 * @return the operator
	 * @throws SQLException per JDBC
	 */
	static Operator getOperatorFromResult(ResultSet resultSet) throws SQLException {
		byte[] operatorBytes = resultSet.getBytes("operator");
		if (Arrays.equals(operatorBytes, consoleUUIDBytes)) {
			return ConsoleOperator.INSTANCE;
		}
		return PlayerOperator.of(UUIDUtil.fromByteArray(operatorBytes));
	}
	
	static class OperatorAdapter implements DataTypeAdapter {
		
		@Override
		public Object adaptObject(Object parameter) {
			if (parameter instanceof Operator) {
				return getOperatorBytes((Operator) parameter);
			}
			return parameter;
		}
		
		private static byte[] getOperatorBytes(Operator operator) {
			switch (operator.getType()) {
			case PLAYER:
				return UUIDUtil.toByteArray(((PlayerOperator) operator).getUUID());
			case CONSOLE:
				return consoleUUIDBytes;
			default:
				throw new IllegalStateException("Unknown operator type " + operator.getType());
			}
		}
		
	}
	
	static class VictimAdapter implements DataTypeAdapter {

		@Override
		public Object adaptObject(Object parameter) {
			if (parameter instanceof Victim) {
				return getVictimBytes((Victim) parameter);
			}
			return parameter;
		}
		
		private static byte[] getVictimBytes(Victim victim) {
			VictimType vType = victim.getType();
			switch (vType) {
			case PLAYER:
				return UUIDUtil.toByteArray(((PlayerVictim) victim).getUUID());
			case ADDRESS:
				return ((AddressVictim) victim).getAddress().getRawAddress();
			default:
				throw new IllegalStateException("Unknown VictimType " + vType);
			}
		}
		
	}
	
	static class ScopeAdapter implements DataTypeAdapter {
		
		private final Scoper scoper;
		
		ScopeAdapter(Scoper scoper) {
			this.scoper = scoper;
		}

		@Override
		public Object adaptObject(Object parameter) {
			if (parameter instanceof ServerScope) {
				return scoper.getServer((ServerScope) parameter);
			}
			return parameter;
		}
		
	}
	
	static class UUIDBytesAdapter implements DataTypeAdapter {
		
		@Override
		public Object adaptObject(Object parameter) {
			if (parameter instanceof UUID) {
				return UUIDUtil.toByteArray((UUID) parameter);
			}
			return parameter;
		}
		
	}
	
	static class NetworkAddressAdapter implements DataTypeAdapter {

		@Override
		public Object adaptObject(Object parameter) {
			if (parameter instanceof NetworkAddress) {
				return ((NetworkAddress) parameter).getRawAddress();
			}
			return parameter;
		}
		
	}
	
}
