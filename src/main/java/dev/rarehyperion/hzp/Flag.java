/*
 * Copyright 2026 RareHyperIon
 *
 * Licensed under the Apache License, Version 2.0 with Commons Clause (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://raw.githubusercontent.com/ByteScan-Labs/HZP/refs/heads/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.rarehyperion.hzp;

public enum Flag {
    PREPENDED_DATA,
    ZIP64,
    MULTI_DISK,
    CD_OFFSET_ANOMALY,
    EOCD_COMMENT,
    SHUFFLED_CENTRAL_DIRECTORY,
    DATA_DESCRIPTOR,
    ENCRYPTED_ENTRY,
    SIZE_MISMATCH,
    DUPLICATE_ENTRY_NAME,
    GHOST_LOCAL_HEADERS,
    SHADOW_LOCAL_HEADERS,
    FAKE_EOCD,
    FAKE_EOCD64,
    MISLEADING_EOCD64_LOCATOR,
    DECOY_EMBEDDED_ZIP,
}