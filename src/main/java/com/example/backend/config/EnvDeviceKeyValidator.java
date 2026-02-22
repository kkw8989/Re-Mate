package com.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnvDeviceKeyValidator implements DeviceKeyValidator {

  private final Environment environment;

  @Override
  public boolean isValid(String deviceKey) {
    String expected = environment.getProperty("security.device.key");
    if (expected == null || expected.isBlank()) {
      return false;
    }
    return expected.equals(deviceKey);
  }
}
