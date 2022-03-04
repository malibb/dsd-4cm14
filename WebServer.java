/*
 *  MIT License
 *
 *  Copyright (c) 2019 Michael Pogrebinsky - Distributed Systems & Cloud Computing with Java
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

//Librerias para construir el servidor
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class WebServer {
    //Cadenas para agregar o quitar endpoints al servidor
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";
    private static final String SEARCH_ENDPOINT = "/search";
    
    //Variable privadas de configuración del servidor
    private final int port;
    private HttpServer server;

    public static void main(String[] args) {
        int serverPort = 8080;//Puerto default
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);//Se asigna el puerto si fue indicado en los argumentos
        }

        WebServer webServer = new WebServer(serverPort);//Instancia de la clase WebServer
        webServer.startServer();//Inicia el método principal que inicializa la configuración del servidor

        System.out.println("Servidor escuchando en el puerto " + serverPort);
    }

    //Constructor que inicializa el puerto
    public WebServer(int port) {
        this.port = port;
    }

    //Método que configura el servidor
    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);//Permimte crear una instancia de socket TCP vinculada a 
            //una IP y un puerto. El segundo parametro se deja en 0 para dejar decidir al sistema el tamaño de la lista de cola de espera de solicitudes pendientes
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        //HttpContext representa un mapeo entre el identificador uniforme de recursos, la aplicación y un HTTPHandler
        //HttpHandler es una interfaz que se invoca cada vez que se procesa una transacción HTTP
        //createContext crea un objeto HttpContext sin un HttpHandler asociado con la ruta relativa asignada
        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);
        HttpContext searchContext = server.createContext(SEARCH_ENDPOINT);

        //setHandler recibe el método que implementa el manejador y vincula el handler para el contexto
        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);
        searchContext.setHandler(this::handleSearchRequest);

        //setExecutor permite crear un objeto Executor que es necesario para el servidor
        server.setExecutor(Executors.newFixedThreadPool(8));//Se deja al ejecutor la labor de iniciar 8 hilos y asignarle tareas
        server.start();//El servidor inicia su ejecución en un hilo en segundo plano
    }
    
    //Método verifica los headers de la petición y ajusta los parámetros para la ruta /task
    private void handleTaskRequest(HttpExchange exchange) throws IOException {//El argumento encapsula todo lo relacionado con la transacción entre servidor y cliente
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {//Se verifica que el método sea post
            exchange.close();//Si no es post, cierra el exchange y sale del método
            return;
        }
        //Condicional, se verifica si la petición contiene la opcion "X-Test" y prepara la respuesta en caso de contenerla
        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "123\n";
            sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }
        //Condicional, se verifica si la petición contiene la opción "X-Debug" y prepara la respuesta en caso de contenerla
        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }
        //Se obitiene una marca de tiempo para conocer la hora de inicio de la petición
        long startTime = System.nanoTime();
        //Se atiende la petición mediante el metodo calculateResponse con los datos leidos
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = calculateResponse(requestBytes);
        //Se obtiene una marca de tiempo para conocer la hora de termino de la petición
        long finishTime = System.nanoTime();
        //Si la opción "X-Debug" estaba activa se añade información a la respuesta
        //Se informa cuanto tardo en realizarse la petición restando las marcas de
        //de tiempo final menos las inicial.
        if (isDebugMode) {
            String debugMessage = String.format("La operación tomó %d nanosegundos", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(responseBytes, exchange);
    }
    
    //Método verifica los headers de la petición y ajusta los parámetros para la ruta /search
    private void handleSearchRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();

        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = calculateResponse(requestBytes);

        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("La operación tomó %d nanosegundos", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(responseBytes, exchange);
    }
    
    //
    private byte[] calculateResponse(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String[] stringNumbers = bodyString.split(",");

        BigInteger result = BigInteger.ONE;

        for (String number : stringNumbers) {
            BigInteger bigInteger = new BigInteger(number);
            result = result.multiply(bigInteger);
        }

        return String.format("El resultado de la multiplicación es %s\n", result).getBytes();
    }
    
    //
    private byte[] search(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String[] stringParams = bodyString.split(",");

        int n = Integer.parseInt(stringParams[0]);
        StringBuilder cadenota = new StringBuilder();
        for(int i = 0; i < n*4; i++){
                if(((i+1)%4) == 0){
                        cadenota.append((byte)32);
                }else{
                        cadenota.append((byte)((Math.random()*(65-90))+90));
                }
        }
        int contador = 0;
        int indice = 0;
        if(stringParams[1] != 3){
            return String.format("La cadena tiene una longitud %s\n", stringParams[1].length).getBytes();   
        }
        String tokenCadenaABuscar= getASCI(stringParams[1]);
        for(int j = 0; j < n*4; j++){
                indice = cadenota.indexOf(tokenCadenaABuscar,j);
                if(indice != -1){
                        indice += 4;
                        contador++;
                        j = indice;
                }else{
                        j += 4;
                }
        }
        return String.format("Incidencias de la cadena %s\n", contador).getBytes();
    }
    
    private String getASCI(String cadena){
        String ascii = "738078";

        return ascii;
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String responseMessage = "El servidor está vivo\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }
}

