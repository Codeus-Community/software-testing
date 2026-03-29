package org.codeus.integration_test;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MainTest {
  @Order(1)
  @Test
  void test() {
    assertTrue(true);
  }
}