package servidorMulti.grupos;

import servidorMulti.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensajeDAO {
    
    public static int enviarMensaje(String usuario, String nombreGrupo, String mensaje) {
        Grupo grupo = GrupoDAO.obtenerGrupo(nombreGrupo);
        
        if (grupo == null) {
            return -1;
        }
        
        if (!GrupoDAO.esMiembro(usuario, nombreGrupo)) {
            return -2;
        }
        
        return insertarMensaje(grupo.getId(), usuario, mensaje);
    }
    
    private static int insertarMensaje(int grupoId, String usuario, String mensaje) {
        String sql = "INSERT INTO mensajes_grupo (grupo_id, usuario, mensaje) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, grupoId);
            pstmt.setString(2, usuario);
            pstmt.setString(3, mensaje);
            pstmt.executeUpdate();
            
            return 1;
            
        } catch (SQLException e) {
            System.err.println("[MENSAJES ERROR] " + e.getMessage());
            return 0;
        }
    }
    
    public static List<MensajeGrupo> obtenerMensajesNoLeidos(String usuario, String nombreGrupo) {
        Grupo grupo = GrupoDAO.obtenerGrupo(nombreGrupo);
        
        if (grupo == null) {
            return new ArrayList<>();
        }
        
        return consultarMensajesNoLeidos(usuario, grupo.getId());
    }
    
    private static List<MensajeGrupo> consultarMensajesNoLeidos(String usuario, int grupoId) {
        List<MensajeGrupo> mensajes = new ArrayList<>();
        String sql = "SELECT m.id, m.grupo_id, m.usuario, m.mensaje, m.fecha " +
                     "FROM mensajes_grupo m " +
                     "WHERE m.grupo_id = ? AND m.usuario != ? " +
                     "AND NOT EXISTS (" +
                     "  SELECT 1 FROM lecturas_mensajes l " +
                     "  WHERE l.mensaje_id = m.id AND l.usuario = ?" +
                     ") ORDER BY m.fecha";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, grupoId);
            pstmt.setString(2, usuario);
            pstmt.setString(3, usuario);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                mensajes.add(crearMensaje(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[MENSAJES ERROR] " + e.getMessage());
        }
        
        return mensajes;
    }
    
    private static MensajeGrupo crearMensaje(ResultSet rs) throws SQLException {
        return new MensajeGrupo(
            rs.getInt("id"),
            rs.getInt("grupo_id"),
            rs.getString("usuario"),
            rs.getString("mensaje"),
            rs.getTimestamp("fecha")
        );
    }
    
    public static void marcarComoLeido(int mensajeId, String usuario) {
        String sql = "INSERT OR IGNORE INTO lecturas_mensajes (mensaje_id, usuario) VALUES (?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, mensajeId);
            pstmt.setString(2, usuario);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[MENSAJES ERROR] " + e.getMessage());
        }
    }
    
    public static void marcarTodosLeidos(String usuario, String nombreGrupo) {
        List<MensajeGrupo> mensajes = obtenerMensajesNoLeidos(usuario, nombreGrupo);
        
        for (MensajeGrupo mensaje : mensajes) {
            marcarComoLeido(mensaje.getId(), usuario);
        }
    }
}