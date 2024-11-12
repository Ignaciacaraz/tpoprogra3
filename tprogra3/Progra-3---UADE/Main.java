import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== SISTEMA DE OPTIMIZACIÓN LOGÍSTICA ===\n");
        
        // Paso 1: Inicializar el grafo y cargar las rutas
        GrafoLogistica grafo = new GrafoLogistica(58); // 50 clientes + 8 centros
        try {
            grafo.cargarRutas("C:/Users/ignac/OneDrive/Escritorio/tprogra3/Progra-3---UADE/rutas.txt/");
        } catch (Exception e) {
            System.err.println("Error al cargar rutas: " + e.getMessage());
            return;
        }

        // Paso 2: Calcular costos mínimos desde cada centro usando Dijkstra
        int[][] costosMinimos = new int[8][50]; // [centro][cliente]
        System.out.println("\nCalculando costos mínimos desde cada centro de distribución...");
        
        for (int centroId = 0; centroId < 8; centroId++) {
            int centroReal = centroId + 50; // Ajuste para ID real del centro (50-57)
            try {
                int[] costosDesdecentro = grafo.dijkstraDesdeCentro(centroReal);
                
                // Guardar solo los costos hacia los clientes (0-49)
                for (int cliente = 0; cliente < 50; cliente++) {
                    costosMinimos[centroId][cliente] = costosDesdecentro[cliente];
                }
                
                System.out.println("Centro " + centroId + ": Costos calculados exitosamente");
            } catch (Exception e) {
                System.err.println("Error al calcular costos para centro " + centroId + ": " + e.getMessage());
                return;
            }
        }

        // Paso 3: Ejecutar la optimización de centros
        System.out.println("\nIniciando optimización de centros de distribución...");
        DistributionCenterOptimization optimizer = new DistributionCenterOptimization();
        
        try {
            optimizer.inicializar("C:/Users/ignac/OneDrive/Escritorio/tprogra3/Progra-3---UADE/clientesYCentros.txt/", costosMinimos);
            SolucionLogistica solucion = optimizer.optimizar();
            
            // Paso 4: Mostrar resultados
            System.out.println("\n=== RESULTADOS DE LA OPTIMIZACIÓN ===");
            System.out.println("Costo total optimizado: " + solucion.getCostoTotal());
            
            System.out.println("\nCentros de distribución seleccionados:");
            for (int i = 0; i < 8; i++) {
                if (solucion.isCentroUtilizado(i)) {
                    System.out.println("Centro " + i);
                }
            }
            
            System.out.println("\nAsignación de clientes:");
            for (int i = 0; i < 50; i++) {
                System.out.println("Cliente " + i + " -> Centro " + 
                                 solucion.getCentroAsignadoCliente(i));
            }
            
        } catch (Exception e) {
            System.err.println("Error durante la optimización: " + e.getMessage());
            return;
        }
    }
}