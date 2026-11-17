package com.training.security;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ApiSaldoServerWithAuth {

    private static final Map<String, Long> dataAkun = new HashMap<>();
    private static final AtomicLong counterRequest = new AtomicLong();
    private static final int PORT = 8088;
    private static final int POOL_SIZE = 20;
    private static final DateTimeFormatter JAM =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) throws IOException {
        siapkanDataAkun();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/saldo", new SaldoHandler());
        server.setExecutor(Executors.newFixedThreadPool(POOL_SIZE));
        server.start();

        System.out.println("=== API Saldo Server WITH AUTH ===");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Berjalan di http://localhost:" + PORT);
        System.out.println("Endpoint: GET /saldo?akun=<nomor>");
        System.out.println();
        System.out.println("Kontrak keamanan:");
        System.out.println("  Setiap request wajib punya header: Authorization: Bearer <token>");
        System.out.println("  Format token: TOKEN-CUST-<N> (contoh: TOKEN-CUST-42)");
        System.out.println("  Token hanya boleh akses akun yang sesuai.");
        System.out.println("  TOKEN-CUST-42 hanya boleh akses AKUN-42, tidak AKUN-1 atau lainnya.");
        System.out.println();
        System.out.println("Status response:");
        System.out.println("  200 = sukses, token valid dan akses akun sendiri");
        System.out.println("  401 = Unauthorized, token hilang atau format salah");
        System.out.println("  403 = Forbidden, token valid tapi akses bukan akun miliknya");
        System.out.println("  404 = akun tidak ada di sistem");
        System.out.println();
        System.out.println("Semua akses dicatat ke audit log di console.");
        System.out.println("Tekan Ctrl+C untuk stop.");
        System.out.println();
    }

    static void siapkanDataAkun() {
        Random rand = new Random(42);
        for (int i = 1; i <= 1000; i++) {
            String nomor = "AKUN-" + i;
            long saldo = 100_000L + (long) (rand.nextDouble() * 50_000_000L);
            dataAkun.put(nomor, saldo);
        }
        System.out.println(dataAkun.size() + " akun dummy disiapkan (AKUN-1 sampai AKUN-1000).");
    }

    static String extractToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) return null;
        if (!header.startsWith("Bearer ")) return null;
        return header.substring("Bearer ".length()).trim();
    }

    static boolean isTokenFormatValid(String token) {
        if (token == null || !token.startsWith("TOKEN-CUST-")) return false;
        try {
            int n = Integer.parseInt(token.substring("TOKEN-CUST-".length()));
            return n >= 1 && n <= 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static String akunMilikToken(String token) {
        return "AKUN-" + token.substring("TOKEN-CUST-".length());
    }

    static void auditLog(long reqId, String token, String akunDiminta, int status, String hasil) {
        String tokenMask = token == null ? "(none)" : token;
        System.out.printf("[%s] [AUDIT] reqId=%d token=%s akun=%s status=%d hasil=%s%n",
            LocalTime.now().format(JAM), reqId, tokenMask, akunDiminta, status, hasil);
    }

    static class SaldoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long reqId = counterRequest.incrementAndGet();
            String akunDiminta = cariParam(exchange.getRequestURI().getRawQuery(), "akun");
            String token = extractToken(exchange);

            if (token == null) {
                String body = "{\"error\":\"authorization header hilang atau format salah\"}";
                auditLog(reqId, null, akunDiminta, 401, "DENY_NO_TOKEN");
                kirim(exchange, 401, body);
                return;
            }

            if (!isTokenFormatValid(token)) {
                String body = "{\"error\":\"token tidak valid\"}";
                auditLog(reqId, token, akunDiminta, 401, "DENY_INVALID_TOKEN");
                kirim(exchange, 401, body);
                return;
            }

            if (akunDiminta == null) {
                String body = "{\"error\":\"parameter akun wajib diisi\"}";
                auditLog(reqId, token, null, 400, "DENY_NO_AKUN_PARAM");
                kirim(exchange, 400, body);
                return;
            }

            String akunMilikToken = akunMilikToken(token);
            if (!akunMilikToken.equals(akunDiminta)) {
                String body = "{\"error\":\"token tidak berhak mengakses akun ini\"}";
                auditLog(reqId, token, akunDiminta, 403, "DENY_FORBIDDEN_CROSS_ACCESS");
                kirim(exchange, 403, body);
                return;
            }

            if (!dataAkun.containsKey(akunDiminta)) {
                String body = "{\"error\":\"akun tidak ditemukan\"}";
                auditLog(reqId, token, akunDiminta, 404, "DENY_AKUN_NOT_FOUND");
                kirim(exchange, 404, body);
                return;
            }

            long saldo = dataAkun.get(akunDiminta);
            String body = String.format("{\"akun\":\"%s\",\"saldo\":%d}", akunDiminta, saldo);
            auditLog(reqId, token, akunDiminta, 200, "ALLOW");
            kirim(exchange, 200, body);
        }

        static void kirim(HttpExchange exchange, int status, String body) throws IOException {
            byte[] data = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Content-Security-Policy", "default-src 'none'");
            exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().add("Server", "ApiSaldoServer/1.2.3 Java/17.0.2");
            exchange.getResponseHeaders().add("X-Powered-By", "Custom Java HttpServer");
            exchange.getResponseHeaders().add("X-Backend-Server", "internal-api-01.prod.btpns.local");
            exchange.sendResponseHeaders(status, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }

        static String cariParam(String query, String nama) {
            if (query == null) return null;
            for (String bagian : query.split("&")) {
                String[] kv = bagian.split("=", 2);
                if (kv.length == 2 && kv[0].equals(nama)) {
                    return kv[1];
                }
            }
            return null;
        }
    }
}
