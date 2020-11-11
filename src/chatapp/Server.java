package chatapp;

import java.sql.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * Sunucu sınıfı, istemciyle bağlantı kurulmasını sağlar.
 * 
 * @author batuhan özdöl
 */
public class Server implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
        
    private final ExecutorService pool;
    
    private Connection connection;
    private ServerSocket serverSocket;

    /**
     * Sunucu sınıfı constructor daha önce fixed olarak (sabit sayıda) tanımlanmıştı.
     * 
     * Thread pool work stealing çalışma yapacak şekilde tanımlandı.
     * Bunun dışında single thread ile tek thread ile işlemler yapılabilirdi.
     * Scheduled thread ile delay ile senkronize olarak işlemler gerçekleştirilebilirdi.
     * Cached thread pool ile uzun sürmeyen işlemler halledilebilirdi.
     */
    public Server() {
        pool = Executors.newWorkStealingPool();
    }
       
    /**
     * Veritabanı işlemleri için veritabanıyla bağlantı kurarak kurulan 
     * bağlantıyı döndüren fonksiyon. Veritabanı sınıfı bulunamaması durumunda 
     * veya veritabanı sorgu mesajında hata olması durumunda oluşan
     * hata loglanır.
     * 
     * Önceki geri bildiriminizde çok sayıda parametre bilgisinin tek tek yerine
     * daha kolay nasıl alınacağını sormuştunuz onu uygulamak için 
     * parametremi değiştirmiştim, bu fonksiyonda uygulamak mantıksız olmuş.
     * 
     * @param driver
     * @param url
     * @param username
     * @param password
     */
    private void createDatabaseConnection(String driver, String url,
            String username, String password) throws ClassNotFoundException, SQLException {       
        Class.forName(driver);
        connection = DriverManager.getConnection(url, username, password);
    } 
    
    /**
     * Fonksiyon, sunucu sınıfının yapacağı işlemlerin farklı Thread kullanarak
     * yapılmasını sağlar. Server soket ve veritabanı bağlantısı kurulup 
     * farklı kullanıcıların bağlanmasını sağlayarak gelen mesajları başarılı 
     * şekilde alındıktan sonra her öncelik için önceden oluşturulan Thread 
     * çalıştırılır.
     * 
     * @param port
     * @param databaseInformation 
     */
    private void start(int port, String ... databaseInformation) {
        System.out.println("Server \n------\n");
        try {
            serverSocket = new ServerSocket(port);
        } 
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while opening server socket", e);
            return;
        }
        /*try {
            createDatabaseConnection(databaseInformation[0], databaseInformation[1],
                databaseInformation[2], databaseInformation[3]);
        }
        catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Cannot connect to database", e);
            return;
        }
        catch(ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Database driver class not found", e);
            return;
        } */        
        // server soket adrese bağlı olduğu sürece çalışmaya devam eder
        while(serverSocket.isBound()) {        
            try {
                Socket socket = serverSocket.accept();
                pool.execute(new ClientService(socket, connection));
            } 
            catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Connection is not accepted from client", e);
                break;
            }             
        }

    }
    
    /**
     * Server nesnesi oluşturulduktan sonra yapacağı işleri hallettikten sonra 
     * veya herhangi hata durumunda en son bu fonksiyon çalışarak kullanılan
     * kaynakların kapanması sağlanır.
     * 
     */
    @Override
    public void close() {  
        pool.shutdown();
        
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Database connection closing failed", ex);  
            }
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                 LOGGER.log(Level.SEVERE, "Server socket closing failed", ex);
            }
        } 
        
    }
        
    /**
     * Fonksiyon sunucu sınıfından bir nesne oluşturarak komut satırından 
     * alınacak bilgiye göre istemci sınıfıyla bağlantı kurup sunucu 
     * tarafının çalışmasını sağlar.
     * 
     * @param args
     */   
    public static void main(String[] args) {
         
        if (args.length != 6) {
            LOGGER.log(Level.SEVERE, "Exactly six parameters required");
            return;
        }
        
        try (Server server = new Server()) {      
            int portNumber = Integer.parseInt(args[1]);
            server.start(portNumber, args[2], args[3], args[4], args[5]);
        }
        catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid arguments",e);
        }
        
    }

}
