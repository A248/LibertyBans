/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
package space.arim.libertybans.it.resolver;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import space.arim.libertybans.api.PunishmentType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class RandomPunishmentTypeResolver extends TypeBasedParameterResolver<PunishmentType> {

	@Override
	public PunishmentType resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Predicate<PunishmentType> acceptability = getAcceptability(parameterContext, extensionContext);

		PunishmentType type;
		do {
			type = randomPunishmentType();
		} while (!acceptability.test(type));
		return type;
	}

	private Predicate<PunishmentType> getAcceptability(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> clazz = extensionContext.getRequiredTestClass();
		var method = extensionContext.getTestMethod();
		// Evaluate SingularPunishment before NotAKick
		if (parameterContext.isAnnotated(SingularPunishment.class)
				|| method.isPresent() && method.get().isAnnotationPresent(SingularPunishment.class)
				|| clazz.isAnnotationPresent(SingularPunishment.class)) {
			return PunishmentType::isSingular;
		}
		if (parameterContext.isAnnotated(NonSingularPunishment.class)) {
			return (type) -> !type.isSingular();
		}
		if (parameterContext.isAnnotated(NotAKick.class)
				|| method.isPresent() && method.get().isAnnotationPresent(NotAKick.class)
				|| clazz.isAnnotationPresent(NotAKick.class)) {
			return (type) -> type != PunishmentType.KICK;
		}
		return (type) -> true;
	}

	public static PunishmentType randomPunishmentType() {
		PunishmentType[] types = PunishmentType.values();
		return types[ThreadLocalRandom.current().nextInt(types.length)];
	}

	/**
	 * Can be placed on a method, parameter, or a type.
	 *
	 * Use of this annotation on a method or parameter overrides a class-level application of {@link NotAKick}.
	 *
	 */
	@Retention(RUNTIME)
	@Target({METHOD, PARAMETER, TYPE})
	public @interface SingularPunishment {

	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public @interface NonSingularPunishment {

	}

	@Retention(RUNTIME)
	@Target({METHOD, PARAMETER, TYPE})
	public @interface NotAKick {

	}
}
