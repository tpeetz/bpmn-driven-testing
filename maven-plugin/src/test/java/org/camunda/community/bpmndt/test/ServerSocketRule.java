package org.camunda.community.bpmndt.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.rules.ExternalResource;

/**
 * JUnit test rule, which receives data from a client socket.
 */
public class ServerSocketRule extends ExternalResource {

  private static final int DEFAULT_PORT = 8001;

  private final int port;

  private Thread thread;
  private ServerSocket serverSocket;
  private String data;

  public ServerSocketRule() {
    this(DEFAULT_PORT);
  }

  public ServerSocketRule(int port) {
    this.port = port;
  }

  @Override
  protected void before() throws Throwable {
    thread = new Thread(this::run);
    thread.start();
  }

  public String getData() {
    return data;
  }

  public int getPort() {
    return port;
  }

  protected void run() {
    try {
      TimeUnit.SECONDS.sleep(1L);
    } catch (InterruptedException e) {
      // can be ignored
    }

    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      throw new RuntimeException("Server socket could not be created", e);
    }

    try {
      Socket socket = serverSocket.accept();

      InputStreamReader r = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);

      StringWriter sw = new StringWriter(512);
      r.transferTo(sw);
      data = sw.toString();

      socket.close();
    } catch (IOException e) {
      throw new RuntimeException("Data could not be read", e);
    } finally {
      try {
        serverSocket.close();
      } catch (IOException e) {
        // can be ignored
      }
    }
  }
}
