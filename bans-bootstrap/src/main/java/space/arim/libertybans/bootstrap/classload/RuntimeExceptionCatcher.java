/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.bootstrap.classload;

import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public final class RuntimeExceptionCatcher implements ClassLoadGuard {

    private final BootstrapLogger logger;
    private final Path reportFolder;
    private final ClassLoadGuard next;

    private final AtomicInteger errorCounter = new AtomicInteger();

    public RuntimeExceptionCatcher(BootstrapLogger logger, Path internalFolder, ClassLoadGuard next) {
        this.logger = Objects.requireNonNull(logger);
        this.reportFolder = internalFolder.resolve("bug-reports");
        this.next = Objects.requireNonNull(next);
        try {
            Files.createDirectories(reportFolder);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public Class<?> delegateLoadClass(String className, boolean resolve, Destination destination) throws ClassNotFoundException {
        try {
            return next.delegateLoadClass(className, resolve, destination);
        } catch (RuntimeException runtimeEx) {

            int errorCount = errorCounter.incrementAndGet();
            if ((errorCount & 0xF) == 1) {
                logger.error(
                        "\n***************************************************************************************\n" +
                                "Caught a " + runtimeEx.getClass().getName() + " during classloading for " + className +
                                " with " + errorCount + " error(s) so far. This bug is highly destabilizing. Please see " +
                                reportFolder + " for details, and copy and send the report there to the relevant people." +
                                "\n***************************************************************************************"
                );
                ForkJoinPool.commonPool().execute(() -> {
                    Path writeTo = reportFolder.resolve(getClass().getName() + '-' + errorCount);
                    try (BufferedWriter writer = Files.newBufferedWriter(writeTo, StandardCharsets.UTF_8);
                         PrintWriter printer = new PrintWriter(writer)) {
                        runtimeEx.printStackTrace(printer);
                    } catch (IOException ex) {
                        runtimeEx.addSuppressed(ex);
                        logger.warn(
                                "Unable to write bug report to file. Printing it here instead.", runtimeEx
                        );
                    }
                });
            }
            throw new ClassNotFoundException("Unexpected runtime error", runtimeEx);
        }
    }

    @Override
    public String toString() {
        return "IllegalStateCatcher{" +
                "reportFolder " + reportFolder +
                ", next=" + next +
                '}';
    }
}
