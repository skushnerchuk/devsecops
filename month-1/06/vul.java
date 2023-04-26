import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class Admin extends HttpServlet {

    protected void post(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Runtime rt = Runtime.getRuntime();
        StringBuilder command = new StringBuilder();
        StringBuilder output = new StringBuilder();

        Process proc = rt.exec(new String[] {"sh", "-c", "ping -c 1 " + req.getParameter("cmd")});
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String s;
        while ((s = stdInput.readLine()) != null) {
            output.append(s);
        }

        while ((s = stdError.readLine()) != null) {
            output.append(s);
        }
        req.setAttribute("output", output.toString());
        req.setAttribute("command", command.toString());
        req.setAttribute("cmd", req.getParameter("cmd"));
        this.doGet(req, resp);
    }
}

