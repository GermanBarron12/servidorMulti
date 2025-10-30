package servidorMulti.grupos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestorGrupos {
    private static Map<String, String> gruposActivos = new HashMap<>();
    
    public static boolean cambiarGrupo(String usuario, String nombreGrupo) {
        if (!GrupoDAO.existeGrupo(nombreGrupo)) {
            return false;
        }
        
        if (!GrupoDAO.esMiembro(usuario, nombreGrupo)) {
            return false;
        }
        
        gruposActivos.put(usuario, nombreGrupo);
        return true;
    }
    
    public static String obtenerGrupoActual(String usuario) {
        return gruposActivos.getOrDefault(usuario, "Todos");
    }
    
    public static void salirGrupo(String usuario) {
        gruposActivos.remove(usuario);
    }
    
    public static String formatearListaGrupos() {
        List<Grupo> grupos = GrupoDAO.listarGrupos();
        
        if (grupos.isEmpty()) {
            return "No hay grupos disponibles";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n==========================================\n");
        sb.append("           GRUPOS DISPONIBLES\n");
        sb.append("==========================================\n");
        
        for (Grupo grupo : grupos) {
            sb.append("  ").append(grupo.toString()).append("\n");
        }
        
        sb.append("==========================================\n");
        return sb.toString();
    }
    
    public static String formatearMisGrupos(String usuario) {
        List<Grupo> grupos = GrupoDAO.listarGruposUsuario(usuario);
        
        if (grupos.isEmpty()) {
            return "No estas en ningun grupo";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n==========================================\n");
        sb.append("            MIS GRUPOS\n");
        sb.append("==========================================\n");
        
        for (Grupo grupo : grupos) {
            sb.append("  ").append(grupo.toString()).append("\n");
        }
        
        sb.append("==========================================\n");
        return sb.toString();
    }
    
    public static String formatearMiembros(String nombreGrupo) {
        List<String> miembros = GrupoDAO.listarMiembros(nombreGrupo);
        
        if (miembros.isEmpty()) {
            return "El grupo no tiene miembros";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n==========================================\n");
        sb.append("    MIEMBROS DEL GRUPO: ").append(nombreGrupo).append("\n");
        sb.append("==========================================\n");
        
        for (String miembro : miembros) {
            sb.append("  ").append(miembro).append("\n");
        }
        
        sb.append("==========================================\n");
        return sb.toString();
    }
}