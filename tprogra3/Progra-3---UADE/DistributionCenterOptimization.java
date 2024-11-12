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
    private int[] costosMinimosPorCliente;
    private int[][] centrosOrdenadosPorCliente;

    public void inicializar(String archivo, int[][] costosCalculados) throws IOException {
        System.out.println("Iniciando inicialización...");
        leerDatosDeArchivo(archivo);
        
        if (costosCalculados == null || costosCalculados.length != numCentros || 
            costosCalculados[0].length != numClientes) {
            throw new IllegalArgumentException("Matriz de costos inválida");
        }

        this.costosTransporte = costosCalculados;
        this.centrosSeleccionados = new boolean[numCentros];
        this.clientesAsignados = new int[numClientes];
        this.mejorAsignacion = new int[numClientes];
        Arrays.fill(clientesAsignados, -1);
        this.costoTotal = Integer.MAX_VALUE;
        
        precalcularCostosMinimosPorCliente();
        precalcularOrdenCentros();
        
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

    private void precalcularCostosMinimosPorCliente() {
        costosMinimosPorCliente = new int[numClientes];
        for (int cliente = 0; cliente < numClientes; cliente++) {
            int costoMinimo = Integer.MAX_VALUE;
            for (int centro = 0; centro < numCentros; centro++) {
                if (costosTransporte[centro][cliente] != Integer.MAX_VALUE) {
                    int costoTotal = costosTransporte[centro][cliente] + centrosCostos[centro][1];
                    costoMinimo = Math.min(costoMinimo, costoTotal);
                }
            }
            costosMinimosPorCliente[cliente] = costoMinimo;
        }
    }

    private void precalcularOrdenCentros() {
        centrosOrdenadosPorCliente = new int[numClientes][numCentros];
        for (int cliente = 0; cliente < numClientes; cliente++) {
            centrosOrdenadosPorCliente[cliente] = obtenerCentrosOrdenadosPorCosto(cliente);
        }
    }

    private void backtracking(int cliente) {
        if (cliente == numClientes) {
            evaluarSolucion();
            return;
        }

        int costoAcumulado = calcularCostoAcumulado(cliente);
        int costoMinimoRestante = calcularCostoMinimoRestante(cliente);
        
        if (costoTotal != Integer.MAX_VALUE && 
            costoAcumulado + costoMinimoRestante >= costoTotal) {
            return;
        }

        int centrosAProbar = Math.min(3, numCentros);
        for (int i = 0; i < centrosAProbar; i++) {
            int centro = centrosOrdenadosPorCliente[cliente][i];
            
            if (costosTransporte[centro][cliente] != Integer.MAX_VALUE && 
                esCentroViable(centro, cliente)) {
                
                clientesAsignados[cliente] = centro;
                centrosSeleccionados[centro] = true;

                backtracking(cliente + 1);

                if (!hayClientesRestantesParaCentro(centro, cliente)) {
                    centrosSeleccionados[centro] = false;
                }
                clientesAsignados[cliente] = -1;
            }
        }
    }

    private int[] obtenerCentrosOrdenadosPorCosto(int cliente) {
        class CentroConCosto implements Comparable<CentroConCosto> {
            int centro;
            int costo;

            CentroConCosto(int centro, int costo) {
                this.centro = centro;
                this.costo = costo;
            }

            @Override
            public int compareTo(CentroConCosto otro) {
                return Integer.compare(this.costo, otro.costo);
            }
        }

        CentroConCosto[] centros = new CentroConCosto[numCentros];
        for (int i = 0; i < numCentros; i++) {
            int costoTotal = costosTransporte[i][cliente];
            if (costoTotal != Integer.MAX_VALUE) {
                costoTotal += centrosCostos[i][1];
            }
            centros[i] = new CentroConCosto(i, costoTotal);
        }

        Arrays.sort(centros);
        return Arrays.stream(centros).mapToInt(c -> c.centro).toArray();
    }

    private boolean esCentroViable(int centro, int cliente) {
        return centrosSeleccionados[centro] || 
               costosTransporte[centro][cliente] <= costosMinimosPorCliente[cliente] * 2;
    }

    private int calcularCostoMinimoRestante(int clienteActual) {
        int costoMinimo = 0;
        for (int i = clienteActual; i < numClientes; i++) {
            costoMinimo += costosMinimosPorCliente[i] * clientesVolumen[i][1];
        }
        int centrosNecesarios = Math.max(1, (numClientes - clienteActual) / 20);
        int costoFijoMinimo = Integer.MAX_VALUE;
        for (int i = 0; i < numCentros; i++) {
            costoFijoMinimo = Math.min(costoFijoMinimo, centrosCostos[i][2]);
        }
        return costoMinimo + (centrosNecesarios * costoFijoMinimo);
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

    private int calcularCostoAcumulado(int clienteActual) {
        int costoAcum = 0;
        int[] clientesPorCentro = new int[numCentros];

        for (int i = 0; i < clienteActual; i++) {
            int centro = clientesAsignados[i];
            if (centro != -1) {
                clientesPorCentro[centro]++;
                costoAcum += clientesVolumen[i][1] * 
                            (costosTransporte[centro][i] + centrosCostos[centro][1]);
            }
        }

        for (int i = 0; i < numCentros; i++) {
            if (clientesPorCentro[i] > 0) {
                costoAcum += centrosCostos[i][2];
            }
        }

        return costoAcum;
    }

    private int calcularCostoTotal() {
        int costo = 0;
        int[] clientesPorCentro = new int[numCentros];

        for (int cliente = 0; cliente < numClientes; cliente++) {
            int centro = clientesAsignados[cliente];
            if (centro != -1) {
                clientesPorCentro[centro]++;
                costo += clientesVolumen[cliente][1] * 
                        (costosTransporte[centro][cliente] + centrosCostos[centro][1]);
            }
        }

        for (int centro = 0; centro < numCentros; centro++) {
            if (clientesPorCentro[centro] > 0) {
                costo += centrosCostos[centro][2];
            }
        }

        return costo;
    }

    private boolean[] obtenerCentrosUtilizados() {
        boolean[] centrosUsados = new boolean[numCentros];
        for (int cliente = 0; cliente < numClientes; cliente++) {
            if (mejorAsignacion[cliente] >= 0 && mejorAsignacion[cliente] < numCentros) {
                centrosUsados[mejorAsignacion[cliente]] = true;
            }
        }
        return centrosUsados;
    }

    private void leerDatosDeArchivo(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            numClientes = Integer.parseInt(br.readLine().split("\t")[0]);
            numCentros = Integer.parseInt(br.readLine().split("\t")[0]);

            centrosCostos = new int[numCentros][3];
            clientesVolumen = new int[numClientes][2];

            for (int i = 0; i < numCentros; i++) {
                String[] datos = br.readLine().split(",");
                centrosCostos[i][0] = Integer.parseInt(datos[0]);
                centrosCostos[i][1] = Integer.parseInt(datos[1]);
                centrosCostos[i][2] = Integer.parseInt(datos[2]);
            }

            for (int i = 0; i < numClientes; i++) {
                String[] datos = br.readLine().split(",");
                clientesVolumen[i][0] = Integer.parseInt(datos[0]);
                clientesVolumen[i][1] = Integer.parseInt(datos[1]);
            }
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
            if (mejorAsignacion[i] != -1) {
                System.out.println("Cliente " + i + 
                                 " (Volumen: " + clientesVolumen[i][1] + ") -> Centro " + 
                                 mejorAsignacion[i] + 
                                 " (Costo transporte: " + costosTransporte[mejorAsignacion[i]][i] + ")");
            }
        }
    }
}

class SolucionLogistica {
    private final int[] asignacionClientes;
    private final int costoTotal;
    private final boolean[] centrosUtilizados;

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