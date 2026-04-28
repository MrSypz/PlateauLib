package com.sypztep.plateau.common.v1.config;

import java.lang.annotation.*;

/**
 * Marks a field on a server config class to be included in the server→client sync handshake.
 *
 * <p>Fields annotated with {@code @RequireSync} are automatically:
 * <ul>
 *   <li>copied field-by-field during {@link ConfigSyncUtil#applyFrom applyFrom()}</li>
 *   <li>included in the hash computed by {@link ConfigSyncUtil#syncHashCode syncHashCode()}</li>
 * </ul>
 *
 * <p>Fields that are {@code static} or {@code final} are silently skipped even if annotated,
 * since they cannot be updated at runtime.
 *
 * <p>Field discovery is cached per class after the first call, so reflection cost is only
 * paid once per config type per JVM session.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * public final class MyServerConfig {
 *
 *     @SerialEntry @RequireSync
 *     public float damageMultiplier = 1.0f;
 *
 *     @SerialEntry @RequireSync
 *     public boolean enableFeature = true;
 *
 *     // Not annotated — local client-only setting, never sent over the network
 *     @SerialEntry
 *     public boolean showDebugHud = false;
 * }
 * }</pre>
 *
 * @see ConfigSyncUtil
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequireSync {}
