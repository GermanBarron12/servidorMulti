package servidorMulti.grupos;

import servidorMulti.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GrupoDAO {
    
    public static int crearGrupo(String nombre, String creador) {
        if (existeGrupo(nombre)) {
            return -1;
        }
        
        String sql = "INSERT INTO grupos (nombre, creador, es_sistema) VALUES (?, ?, 0)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombre);
            pstmt.setString(2, creador);
            pstmt.executeUpdate();
            
            System.out.println("[GRUPOS] Grupo creado: " + nombre);
            return 1;
            
        } catch (SQLException e) {
            System.err.println("[GRUPOS ERROR] " + e.getMessage());
            return 0;
        }
    }
    
    public static boolean existeGrupo(String nombre) {
        String sql = "SELECT COUNT(*) FROM grupos WHERE nombre = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next() && rs.getInt(1) > 0;
            
        } catch (SQLException e) {
            return false;
        }
    }
    
    public static Grupo obtenerGrupo(String nombre) {
        String sql = "SELECT id, nombre, creador, es_sistema FROM grupos WHERE nombre = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Grupo(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("creador"),
                    rs.getInt("es_sistema") == 1
                );
            }
            
        } catch (SQLException e) {
            System.err.println("[GRUPOS ERROR] " + e.getMessage());
        }
        
        return null;
    }
    
    public static int eliminarGrupo(String nombre, String usuario) {
        Grupo grupo = obtenerGrupo(nombre);
        
        if (grupo == null) {
            return -1;
        }
        
        if (grupo.isEsSistema()) {
            return -2;
        }
        
        if (!grupo.getCreador().equals(usuario)) {
            return -3;
        }
        
        return ejecutarEliminacion(grupo.getId());
    }
    
    private static int ejecutarEliminacion(int grupoId) {
        String sqlMiembros = "DELETE FROM miembros_grupo WHERE grupo_id = ?";
        String sqlMensajes = "DELETE FROM mensajes_grupo WHERE grupo_id = ?";
        String sqlGrupo = "DELETE FROM grupos WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            ejecutarDelete(conn, sqlMiembros, grupoId);
            ejecutarDelete(conn, sqlMensajes, grupoId);
            ejecutarDelete(conn, sqlGrupo, grupoId);
            
            conn.commit();
            System.out.println("[GRUPOS] Grupo eliminado: ID " + grupoId);
            return 1;
            
        } catch (SQLException e) {
            System.err.println("[GRUPOS ERROR] " + e.getMessage());
            return 0;
        }
    }
    
    private static void ejecutarDelete(Connection conn, String sql, int grupoId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, grupoId);
            pstmt.executeUpdate();
        }
    }
    
    public static List<Grupo> listarGrupos() {
        List<Grupo> grupos = new ArrayList<>();
        String sql = "SELECT id, nombre, creador, es_sistema FROM grupos ORDER BY nombre";
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                grupos.add(new Grupo(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("creador"),
                    rs.getInt("es_sistema") == 1
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("[GRUPOS ERROR] " + e.getMessage());
        }
        
        return grupos;
    }
    
    public static int unirseGrupo(String usuario, String nombreGrupo) {
        Grupo grupo = obtenerGrupo(nombreGrupo);
        
        if (grupo == null) {
            return -1;
        }
        
        if (esMiembro(usuario, grupo.getId())) {
            return -2;
        }
        
        return insertarMiembro(usuario, grupo.getId());
    }
    
    private static int insertarMiembro(String usuario, int grupoId) {
        String sql = "INSERT INTO miembros_grupo (grupo_id, usuario) VALUES (?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, grupoId);
            pstmt.setString(2, usuario);
            pstmt.executeUpdate();
            
            System.out.println("[GRUPOS] " + usuario + " se unio al grupo ID " + grupoId);
            return 1;
            
        } catch (SQLException e) {
            System.err.println("[GRUPOS ERROR] " + e.getMessage());
            return 0;
        }
    }
    
    public static boolean esMiembro(String usuario, String nombreGrupo) {
        Grupo grupo = obtenerGrupo(nombreGrupo);
        return grupo != null && esMiembro(usuario, grupo.getId());
    }
    
    private static boolean esMiembro(String usuario, int grupoId) {
        String sql = "SELECT COUNT(*) FROM miembros_grupo WHERE grupo_id = ? AND usuario = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, grupoId);
            pstmt.setString(2, usuario);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next() && rs.getInt(1) > 0;
            
        } catch (SQLException e) {
            return false;
        }
    }
    
    public static List<String> listarMiembros(String nombreGrupo) {
        List<String> miembros = new ArrayList<>();
        Grupo grupo = obtenerGrupo(nombreGrupo);
        
        if (grupo == null) {
            return miembros;
        }
        
        String sql = "SELECT usuario FROM miembros_grupo WHERE grupo_id = ? ORDER BY usuario";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, grupo.getId());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                miembros.add(rs.getString("usuario"));
            }
            
        } catch (SQLException e) {
            System.err.println("[GRUPOS ERROR] " + e.getMessage());
        }
        
        return miembros;
    }
    
    public static List<Grupo> listarGruposUsuario(String usuario) {
        List<Grupo> grupos = new ArrayList<>();
        String sql = "SELECT g.id, g.nombre, g.creador, g.es_sistema " +
                     "FROM grupos g INNER JOIN miembros_grupo mg ON g.id = mg.grupo_id " +
                     "WHERE mg.usuario = ? ORDER BY g.nombre";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                grupos.add(new Grupo(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("creador"),
                    rs.getInt("es_sistema") == 1
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("[GRUPOS ERROR] " + e.getMessage());
        }
        
        return grupos;
    }
}