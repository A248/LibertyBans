/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.core.punish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.CalculablePunishment;
import space.arim.libertybans.api.punish.CalculablePunishmentBuilder;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.DraftSanction;
import space.arim.libertybans.api.punish.DraftSanctionBuilder;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.PunishmentDetailsCalculator;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.scope.GlobalScope;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.scope.SpecificServerScope;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PunishmentBuilderTest {

    private final Enactor enactor;
    private final InternalScopeManager scopeManager;

    private final Map<Class<?>, Object> builderParameters = new HashMap<>();

    public PunishmentBuilderTest(@Mock Enactor enactor, @Mock InternalScopeManager scopeManager) {
        this.enactor = enactor;
        this.scopeManager = scopeManager;
    }

    private <T> void addBuilderParam(Class<T> clazz, T value) {
        builderParameters.put(clazz, value);
    }

    @BeforeEach
    public void setup() {
        when(enactor.scopeManager()).thenReturn(scopeManager);
        when(scopeManager.globalScope()).thenReturn(GlobalScope.INSTANCE);
        lenient().when(scopeManager.checkScope(any())).thenAnswer((i) -> i.getArgument(0));

        addBuilderParam(Victim.class, PlayerVictim.of(UUID.randomUUID()));
        addBuilderParam(Operator.class, PlayerOperator.of(UUID.randomUUID()));
        addBuilderParam(EscalationTrack.class, EscalationTrack.create("hi", "there"));
    }

    private <B extends DraftSanctionBuilder<B, D>, D extends DraftSanction> void testBuilder(
            Class<B> builderClass, Class<D> sanctionClass, B builder) {
        for (Method method : builderClass.getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (!DraftSanctionBuilder.class.isAssignableFrom(returnType)) {
                continue;
            }
            Class<?> argType = method.getParameterTypes()[0];
            Object argValue = builderParameters.get(argType);
            if (argValue == null) {
                // The test should thus fail
                continue;
            }
            argType.cast(argValue);
            try {
                Object newBuilder = method.invoke(builder, argValue);
                builder = builderClass.cast(newBuilder);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        D built = builder.build();
        Object backToBuilder;
        try {
            Method toBuilderMethod = sanctionClass.getMethod("toBuilder");
            backToBuilder = toBuilderMethod.invoke(built);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        D rebuilt = builderClass.cast(backToBuilder).build();
		assertEquals(built, rebuilt);
    }

    @Test
    public void draftPunishmentBuilderBuild() {
        when(enactor.draftBuilder()).thenAnswer((i) -> new DraftPunishmentBuilderImpl(enactor));

        addBuilderParam(PunishmentType.class, PunishmentType.BAN);
        addBuilderParam(String.class, "Test");
        addBuilderParam(ServerScope.class, new SpecificServerScope("serveme!"));
        addBuilderParam(Duration.class, Duration.ofHours(3L));

        testBuilder(
                DraftPunishmentBuilder.class, DraftPunishment.class, enactor.draftBuilder()
        );
    }

    @Test
    public void calculablePunishmentBuilderBuild() {
        when(enactor.calculablePunishmentBuilder()).thenReturn(new CalculablePunishmentBuilderImpl(enactor));

        PunishmentDetailsCalculator calculator = (track, victim, selectionOrderBuilder) -> null;
        addBuilderParam(PunishmentDetailsCalculator.class, calculator);

        testBuilder(
                CalculablePunishmentBuilder.class, CalculablePunishment.class, enactor.calculablePunishmentBuilder()
        );
    }

}
