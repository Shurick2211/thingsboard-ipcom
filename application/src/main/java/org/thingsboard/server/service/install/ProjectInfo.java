// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectInfo {

    private final Optional<BuildProperties> buildProperties;

    @Value("${app.version:unknown}")
    private String appVersion;

    public String getProjectVersion() {
        String version = buildProperties.map(BuildProperties::getVersion)
                .or(() -> Optional.ofNullable(appVersion)
                        .filter(v -> !v.isEmpty() && !v.startsWith("@") && !"unknown".equals(v)))
                .orElse("4.4.0");
        return version.replaceAll("[^\\d.]", "");
    }

    public String getProductType() {
        return "CE";
    }

}
