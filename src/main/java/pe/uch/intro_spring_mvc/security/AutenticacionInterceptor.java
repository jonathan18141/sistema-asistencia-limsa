package pe.uch.intro_spring_mvc.security;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Exige inicio de sesion y, para aprobar marcas o registrar empleados, el rol RRHH. */
public class AutenticacionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        String usuario = session == null ? null : (String) session.getAttribute("usuario");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        boolean accionDeRrhh = ruta.startsWith("/reporte/aprobar") || ruta.startsWith("/empleados");
        if (accionDeRrhh && !"RRHH".equals(session.getAttribute("rolUsuario"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo RRHH puede realizar esta acción.");
            return false;
        }
        return true;
    }
}
