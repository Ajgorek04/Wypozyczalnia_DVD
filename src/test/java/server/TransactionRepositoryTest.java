// java
package server;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class TransactionRepositoryExtraTest {

    @Test
    void concurrentRent_onlyOneSucceeds() throws Exception {
        UserRepository ur = new UserRepository();
        String u1 = "uA_" + System.nanoTime();
        String u2 = "uB_" + System.nanoTime();
        assertTrue(ur.registerUser(u1, "p"));
        assertTrue(ur.registerUser(u2, "p"));
        final int id1 = ur.getUserIdByCredentials(u1, "p");
        final int id2 = ur.getUserIdByCredentials(u2, "p");
        assertTrue(id1 > 0 && id2 > 0);

        final int filmId;
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM Film WHERE dostepny=TRUE LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) filmId = rs.getInt(1);
            else filmId = -1;
        }
        assertTrue(filmId > 0);

        final TransactionRepository tx = new TransactionRepository();
        ExecutorService ex = Executors.newFixedThreadPool(2);
        final AtomicInteger successCount = new AtomicInteger(0);
        Callable<Void> task1 = () -> { if (tx.rentFilm(id1, filmId)) successCount.incrementAndGet(); return null; };
        Callable<Void> task2 = () -> { if (tx.rentFilm(id2, filmId)) successCount.incrementAndGet(); return null; };

        Future<Void> f1 = ex.submit(task1);
        Future<Void> f2 = ex.submit(task2);
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        ex.shutdownNow();

        assertEquals(1, successCount.get(), "Tylko jedno wypożyczenie powinno się udać");

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT dostepny FROM Film WHERE id = ?")) {
            ps.setInt(1, filmId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertFalse(rs.getBoolean(1), "Film powinien być niedostępny po udanym wypożyczeniu");
            }
        }
    }
}
