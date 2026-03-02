package serviceTests;

import dataaccess.*;
import model.UserData;
import org.junit.jupiter.api.*;
import service.ClearService;

public class ClearServiceTest {
    @Test
    @DisplayName("Clear Success")
    public void clearSuccess() {
        var u = new MemoryUserDAO();
        var a = new MemoryAuthDAO();
        var g = new MemoryGameDAO();
        var service = new ClearService(u, a, g);

        u.createUser(new UserData("u", "p", "e"));
        service.clear();

        Assertions.assertNull(u.getUser("u"));
    }
}