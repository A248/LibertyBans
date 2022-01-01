/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.importing;

import org.jooq.CloseableDSLContext;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalDatabaseSetup implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

	private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(getClass());

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Hsqldb {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface H2 {

	}

	private boolean isHsqldbNotH2(Class<?> testClass) {
		if (testClass.getAnnotation(Hsqldb.class) != null) {
			return true;
		}
		if (testClass.getAnnotation(H2.class) != null) {
			return false;
		}
		throw new ExtensionConfigurationException("Either @Hsqldb or @H2 is required");
	}

	private record WrappedDSLContext(CloseableDSLContext context) implements ExtensionContext.Store.CloseableResource {

		@Override
		public void close() throws Throwable {
			context.close();
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		int counterValue;
		{
			AtomicInteger globalCounter = context.getRoot().getStore(namespace)
					.getOrComputeIfAbsent(AtomicInteger.class, (k) -> new AtomicInteger(), AtomicInteger.class);
			counterValue = globalCounter.getAndIncrement();
		}
		String jdbcUrl =  isHsqldbNotH2(context.getRequiredTestClass()) ?
				"jdbc:hsqldb:mem:testdb-" + counterValue
				: "jdbc:h2:mem:testdb-" + counterValue + ";DB_CLOSE_DELAY=-1";

		context.getStore(namespace).put(String.class, jdbcUrl);
		context.getStore(namespace).put(WrappedDSLContext.class, new WrappedDSLContext(DSL.using(jdbcUrl, "SA", "")));
	}

	private ConnectionSource getConnectionSource(ExtensionContext context) {
		String jdbcUrl = context.getStore(namespace).get(String.class, String.class);
		return () -> DriverManager.getConnection(jdbcUrl, "SA", "");
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		// Both HSQLDB and H2 use the 'SHUTDOWN' statement
		try (Connection connection = getConnectionSource(context).openConnection();
			 PreparedStatement prepStmt = connection.prepareStatement("SHUTDOWN")) {
			prepStmt.execute();
		}
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		var paramType = parameterContext.getParameter().getType();
		return paramType.equals(ConnectionSource.class) || paramType.equals(DSLContext.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		var paramType = parameterContext.getParameter().getType();
		if (paramType.equals(DSLContext.class)) {
			return extensionContext.getStore(namespace).get(WrappedDSLContext.class, WrappedDSLContext.class).context;
		}
		assert paramType.equals(ConnectionSource.class);
		return getConnectionSource(extensionContext);
	}
}
