import java.io.*;
import java.util.*;

public class GrafoLogistica {
    // Clase interna para representar las conexiones
    public class Arista {
        int destino;
        int costo;

        public Arista(int destino, int costo) {
            this.destino = destino;
            this.costo = costo;
        }
    }

    public ArrayList<ArrayList<Arista>> listaAdyacencia; // Lista de conexiones
    public int numNodos;  // Total de nodos (clientes + centros)

    public GrafoLogistica(int numNodos) {
        this.numNodos = numNodos;
        // Inicializa la lista de adyacencia
        listaAdyacencia = new ArrayList<>(numNodos);
        // Crea una lista vacía para cada nodo
        for (int i = 0; i < numNodos; i++) {
            listaAdyacencia.add(new ArrayList<>());
        }
    }

    public void cargarRutas(String rutasFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutasFile))) {
            // Lee la primera línea que contiene el total de rutas
            int totalRutas = Integer.parseInt(br.readLine().split("\t")[0]);
            System.out.println("Total de rutas a cargar: " + totalRutas);

            String linea;
            int rutasCargadas = 0;
            // Lee cada línea del archivo
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(",");
                if (datos.length == 3) {
                    // Extrae origen, destino y costo
                    int origen = Integer.parseInt(datos[0]);
                    int destino = Integer.parseInt(datos[1]);
                    int costo = Integer.parseInt(datos[2]);

                    // Agrega la ruta al grafo
                    agregarRuta(origen, destino, costo);
                    rutasCargadas++;
                }
            }
            System.out.println("Rutas cargadas exitosamente: " + rutasCargadas);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de rutas: " + e.getMessage());
        }
    }

    public void agregarRuta(int origen, int destino, int costo) {
        // Agrega una nueva arista a la lista de adyacencia del nodo origen
        listaAdyacencia.get(origen).add(new Arista(destino, costo));
    }

    // Obtener todos los vecinos de un nodo
    public List<Arista> getVecinos(int nodo) {
        return listaAdyacencia.get(nodo);
    }

    // Verificar si existe una conexión directa entre dos nodos
    public boolean existeRutaDirecta(int origen, int destino) {
        for (Arista arista : listaAdyacencia.get(origen)) {
            if (arista.destino == destino) {
                return true;
            }
        }
        return false;
    }

    // Obtener el costo entre dos nodos conectados
    public int getCosto(int origen, int destino) {
        for (Arista arista : listaAdyacencia.get(origen)) {
            if (arista.destino == destino) {
                return arista.costo;
            }
        }
        return -1; // Si no existe conexión directa
    }

    // Método para calcular costos desde un centro de distribución hacia todos los clientes
    public int[] dijkstraDesdeCentro(int centro) {
        // Verifica que el nodo de origen sea un centro de distribución
        if (centro < 50 || centro > 57) {
            throw new IllegalArgumentException("El nodo debe ser un centro de distribución (50-57).");
        }

        // Arreglo para almacenar el costo mínimo desde el centro a cada cliente
        int[] costoMinimo = new int[numNodos];
        Arrays.fill(costoMinimo, Integer.MAX_VALUE); // Inicializa con costos infinitos
        costoMinimo[centro] = 0; // Costo del nodo origen a sí mismo es 0

        // Cola de prioridad para seleccionar el nodo con el costo más bajo
        PriorityQueue<Arista> cola = new PriorityQueue<>(Comparator.comparingInt(arista -> arista.costo));
        cola.add(new Arista(centro, 0));

        while (!cola.isEmpty()) {
            Arista nodoActual = cola.poll();
            int nodoId = nodoActual.destino;

            // Explorar los vecinos del nodo actual
            for (Arista vecino : getVecinos(nodoId)) {
                int nuevoCosto = costoMinimo[nodoId] + vecino.costo;

                // Si encontramos un costo más bajo, actualizamos el arreglo y la cola
                if (nuevoCosto < costoMinimo[vecino.destino]) {
                    costoMinimo[vecino.destino] = nuevoCosto;
                    cola.add(new Arista(vecino.destino, nuevoCosto));
                }
            }
        }

        return costoMinimo; // Devuelve el arreglo de costos mínimos hacia todos los nodos
    }

    public void imprimirCostos(int[] costos) {
        System.out.println("Costos desde el centro de distribución:");
        for (int cliente = 0; cliente < 50; cliente++) {
            if (costos[cliente] == Integer.MAX_VALUE) {
                System.out.println("Cliente " + cliente + ": No alcanzable");
            } else {
                System.out.println("Cliente " + cliente + ": Costo " + costos[cliente]);
            }
        }
    }
}
