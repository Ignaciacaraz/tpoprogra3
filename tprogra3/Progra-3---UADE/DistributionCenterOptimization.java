import java.io.*;
import java.util.*;

public class DistributionCenterOptimization {
    private int numClientes;
    private int numCentros;
    private int[][] centrosCostos;      // [centro][costoUnitarioPuerto, costoFijo]
    private int[][] clientesVolumen;     // [cliente][volumen]
    private int[][] costosTransporte;    // [centro][cliente] - calculado por Dijkstra
    private boolean[] centrosSeleccionados;
    private int[] mejorAsignacion;
    private int costoTotal;
    private int[] clientesAsignados;

    public void inicializar(String archivo, int[][] costosCalculados) throws IOException {
        System.out.println("Iniciando inicialización...");
        leerDatosDeArchivo(archivo);
        
        System.out.println("Datos leídos: " + numClientes + " clientes, " + numCentros + " centros");
        
        if (costosCalculados == null) {
            throw new IllegalArgumentException("La matriz de costos calculados es null");
        }
        if (costosCalculados.length != numCentros) {
            throw new IllegalArgumentException("La matriz de costos no tiene el número correcto de centros. Esperado: " + numCentros + ", Recibido: " + costosCalculados.length);
        }
        if (costosCalculados[0].length != numClientes) {
            throw new IllegalArgumentException("La matriz de costos no tiene el número correcto de clientes. Esperado: " + numClientes + ", Recibido: " + costosCalculados[0].length);
        }

        this.costosTransporte = costosCalculados;
        this.centrosSeleccionados = new boolean[numCentros];
        this.clientesAsignados = new int[numClientes];
        this.mejorAsignacion = new int[numClientes];
        Arrays.fill(clientesAsignados, -1);
        this.costoTotal = Integer.MAX_VALUE;
        
        System.out.println("Inicialización completada exitosamente");
    }

    public SolucionLogistica optimizar() {
        System.out.println("Iniciando proceso de optimización...");
        try {
            backtracking(0);
            
            if (costoTotal == Integer.MAX_VALUE) {
                throw new RuntimeException("No se encontró ninguna solución válida");
            }
            
            System.out.println("Optimización completada. Costo total encontrado: " + costoTotal);
            return new SolucionLogistica(
                mejorAsignacion.clone(),
                costoTotal,
                obtenerCentrosUtilizados()
            );
        } catch (Exception e) {
            System.err.println("Error durante la optimización: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private boolean[] obtenerCentrosUtilizados() {
        boolean[] centrosUsados = new boolean[numCentros];
        for (int cliente = 0; cliente < numClientes; cliente++) {
            if (mejorAsignacion[cliente] >= 0 && mejorAsignacion[cliente] < numCentros) {
                centrosUsados[mejorAsignacion[cliente]] = true;
            } else {
                System.err.println("Error: Asignación inválida para cliente " + cliente + ": " + mejorAsignacion[cliente]);
            }
        }
        return centrosUsados;
    }

    private void backtracking(int cliente) {
        
        // Si ya asignamos todos los clientes, evaluamos la solución
        if (cliente == numClientes) {
            evaluarSolucion();
            return;
        }

        // Para cada cliente, probamos asignarlo a cada centro disponible
        for (int centro = 0; centro < numCentros; centro++) {
            try {
                // Solo consideramos centros que tienen una ruta válida al cliente
                if (costosTransporte[centro][cliente] != Integer.MAX_VALUE) {
                    
                    // Si el centro no estaba seleccionado, lo marcamos
                    boolean centroNuevo = !centrosSeleccionados[centro];
                    
                    // Asignamos el cliente al centro
                    clientesAsignados[cliente] = centro;
                    centrosSeleccionados[centro] = true;

                    // Verificamos si la solución parcial actual es prometedora
                    if (esSolucionParcialPrometedora(cliente, centro)) {
                        backtracking(cliente + 1);
                    }

                    // Deshacemos la asignación si el centro era nuevo y no hay más clientes asignados
                    if (centroNuevo && !hayClientesRestantesParaCentro(centro, cliente)) {
                        centrosSeleccionados[centro] = false;
                    }
                    
                    // Si no encontramos una solución válida, dejamos al cliente sin asignar
                    clientesAsignados[cliente] = -1;
                }
            } catch (Exception e) {
                System.err.println("Error procesando cliente " + cliente + " con centro " + centro);
                System.err.println("Estado actual: " + Arrays.toString(clientesAsignados));
                e.printStackTrace();
                throw e;
            }
        }
    }

    private boolean esSolucionParcialPrometedora(int clienteActual, int centroAsignado) {
        try {
            int costoActualParcial = 0;
            boolean[] centrosUsadosParcial = new boolean[numCentros];
            
            // Calculamos el costo de las asignaciones realizadas hasta ahora
            for (int i = 0; i <= clienteActual; i++) {
                int centro = clientesAsignados[i];
                if (centro != -1) {
                    centrosUsadosParcial[centro] = true;
                    int costoTransporte = costosTransporte[centro][i];
                    int costoUnitarioPuerto = centrosCostos[centro][1];
                    int volumenCliente = clientesVolumen[i][1];
                    
                    costoActualParcial += volumenCliente * (costoTransporte + costoUnitarioPuerto);
                }
            }
            
            // Agregamos los costos fijos de los centros utilizados
            for (int i = 0; i < numCentros; i++) {
                if (centrosUsadosParcial[i]) {
                    costoActualParcial += centrosCostos[i][2];
                }
            }
            
            return costoActualParcial < costoTotal;
        } catch (Exception e) {
            System.err.println("Error evaluando solución parcial para cliente " + clienteActual + " y centro " + centroAsignado);
            e.printStackTrace();
            throw e;
        }
    }

    private boolean hayClientesRestantesParaCentro(int centro, int clienteActual) {
        for (int i = 0; i <= clienteActual; i++) {
            if (clientesAsignados[i] == centro) {
                return true;
            }
        }
        return false;
    }

    private void evaluarSolucion() {
        int costoActual = calcularCostoTotal();
        if (costoActual < costoTotal) {
            costoTotal = costoActual;
            System.arraycopy(clientesAsignados, 0, mejorAsignacion, 0, numClientes);
            System.out.println("Nueva mejor solución encontrada con costo: " + costoTotal);
        }
    }

    private int calcularCostoTotal() {
        try {
            int costo = 0;
            boolean[] centrosUsados = new boolean[numCentros];

            // Calcular costos de transporte y operación
            for (int cliente = 0; cliente < numClientes; cliente++) {
                int centro = clientesAsignados[cliente];
                if (centro != -1) {
                    centrosUsados[centro] = true;
                    // Costo de transporte cliente -> centro -> puerto
                    costo += clientesVolumen[cliente][1] * 
                            (costosTransporte[centro][cliente] + centrosCostos[centro][1]);
                }
            }

            // Agregar costos fijos de los centros utilizados
            for (int centro = 0; centro < numCentros; centro++) {
                if (centrosUsados[centro]) {
                    costo += centrosCostos[centro][2];
                }
            }

            return costo;
        } catch (Exception e) {
            System.err.println("Error calculando costo total");
            System.err.println("Estado de asignación: " + Arrays.toString(clientesAsignados));
            e.printStackTrace();
            throw e;
        }
    }

    private void leerDatosDeArchivo(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            // Leer número de clientes y centros
            numClientes = Integer.parseInt(br.readLine().split("\t")[0]);
            numCentros = Integer.parseInt(br.readLine().split("\t")[0]);

            System.out.println("Leyendo datos para " + numClientes + " clientes y " + numCentros + " centros");

            // Inicializar arrays
            centrosCostos = new int[numCentros][3];
            clientesVolumen = new int[numClientes][2];

            // Leer datos de los centros
            for (int i = 0; i < numCentros; i++) {
                String[] datos = br.readLine().split(",");
                centrosCostos[i][0] = Integer.parseInt(datos[0]); // ID del centro
                centrosCostos[i][1] = Integer.parseInt(datos[1]); // Costo unitario al puerto
                centrosCostos[i][2] = Integer.parseInt(datos[2]); // Costo fijo anual
                System.out.println("Centro " + i + " leído: ID=" + centrosCostos[i][0] + 
                                 ", CostoUnitario=" + centrosCostos[i][1] + 
                                 ", CostoFijo=" + centrosCostos[i][2]);
            }

            // Leer datos de los clientes
            for (int i = 0; i < numClientes; i++) {
                String[] datos = br.readLine().split(",");
                clientesVolumen[i][0] = Integer.parseInt(datos[0]); // ID del cliente
                clientesVolumen[i][1] = Integer.parseInt(datos[1]); // Volumen de producción
                System.out.println("Cliente " + i + " leído: ID=" + clientesVolumen[i][0] + 
                                 ", Volumen=" + clientesVolumen[i][1]);
            }

        } catch (IOException e) {
            System.err.println("Error al leer el archivo " + filename + ": " + e.getMessage());
            throw e;
        } catch (NumberFormatException e) {
            System.err.println("Error al parsear números del archivo: " + e.getMessage());
            throw e;
        }
    }

    public void imprimirSolucion() {
        System.out.println("\n=== SOLUCIÓN ENCONTRADA ===");
        System.out.println("Costo total: " + costoTotal);
        
        System.out.println("\nCentros utilizados:");
        boolean[] centrosUsados = obtenerCentrosUtilizados();
        for (int i = 0; i < numCentros; i++) {
            if (centrosUsados[i]) {
                System.out.println("Centro " + i + 
                                 " (Costo fijo: " + centrosCostos[i][2] + 
                                 ", Costo unitario puerto: " + centrosCostos[i][1] + ")");
            }
        }
        
        System.out.println("\nAsignación de clientes:");
        for (int i = 0; i < numClientes; i++) {
            System.out.println("Cliente " + i + 
                             " (Volumen: " + clientesVolumen[i][1] + ") -> Centro " + 
                             mejorAsignacion[i] + 
                             " (Costo transporte: " + costosTransporte[mejorAsignacion[i]][i] + ")");
        }
    }
}

class SolucionLogistica {
    private final int[] asignacionClientes;    // Qué centro atiende a cada cliente
    private final int costoTotal;              // Costo total de la solución
    private final boolean[] centrosUtilizados; // Qué centros se utilizan

    public SolucionLogistica(int[] asignacionClientes, int costoTotal, boolean[] centrosUtilizados) {
        this.asignacionClientes = asignacionClientes;
        this.costoTotal = costoTotal;
        this.centrosUtilizados = centrosUtilizados;
    }

    public int[] getAsignacionClientes() {
        return asignacionClientes;
    }

    public int getCostoTotal() {
        return costoTotal;
    }

    public boolean[] getCentrosUtilizados() {
        return centrosUtilizados;
    }

    public int getCentroAsignadoCliente(int cliente) {
        return asignacionClientes[cliente];
    }

    public boolean isCentroUtilizado(int centro) {
        return centrosUtilizados[centro];
    }
}