package chatapp;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sunucu sınıfının kurduğu bağlantı sonucu istemciye burada hizmet verilir. 
 * 
 * @author batuh
 */
public class ClientService implements Runnable {
        
    private static final Logger LOGGER = Logger.getLogger(ClientService.class.getName());
    private static final String QUERYHEAD = "insert into ";
    private static final String QUERYEND = "_oncelik (`to`, `cc`, `subject`, `priority`) values (?,?,?,?)";
    
    private final Socket socket;
    private final Connection connection;
    
    private Queue<Message> lowPriorityQueue;
    private Queue<Message> normalPriorityQueue;
    private Queue<Message> highPriorityQueue;
    private PrintWriter pr;
    private BufferedReader br;
    private ObjectOutputStream lowStream;
    private ObjectOutputStream normalStream;
    private ObjectOutputStream highStream;
        
    /**
     * Sınıf oluşturan constructor
     * @param socket
     * @param connection
     */
    public ClientService(Socket socket, Connection connection) {
        this.socket = socket;
        this.connection = connection;
    }   
       
    
    /**
     * Sunucu tarafında {@link Message} nesnesinin öncelik bilgilerine 
     * göre ilgili tablolara ekleyen fonksiyon. Veritabanında sorgu hatası 
     * olması durumunda hata mesajı burada loglanır.
     * 
     * @param message
     * @exception SQLException 
     */
    private void insertMessageToDatabase(Message message) throws SQLException {
        PreparedStatement pst = connection.prepareStatement
        (QUERYHEAD + message.getPriority().toString().toLowerCase() + QUERYEND);
        pst.setString(1, message.getTo());
        pst.setString(2, message.getCc());
        pst.setString(3, message.getSubject());
        pst.setString(4, message.getPriority().toString().toLowerCase());
        pst.executeUpdate();
    }
        
    /**
     * {@link Message} nesnesinin sunucu tarafına ulaştığının cevabı olarak
     * istemci tarafına bir cevap yollanır.
     * 
     * @param message 
     */
    private void responseToClient(Message message) {
        System.out.println("Message about '" + message.getSubject() + "' was delivered. \n"); 
        pr.println("Message is taken");  
        pr.flush();
    }
        
    /**
     * Alınan {@link Message} nesnesinin öncelik durumu uygun olması
     * koşuluyla ilgili önceliğe sahip dosyaya yazılması sağlanır. . 
     * 
     * @param message
     * @exception IOException 
     */
    private void writeMessageToFile(Message message) throws IOException {

        if (message.getPriority().toString().equalsIgnoreCase(Priority.DUSUK.toString())) {        
            lowStream.writeObject(message);
        }
        else if (message.getPriority().toString().equalsIgnoreCase(Priority.NORMAL.toString())) {
            normalStream.writeObject(message);
        } 
        else if (message.getPriority().toString().equalsIgnoreCase(Priority.YUKSEK.toString()))  {
            highStream.writeObject(message);
        }

    }
    
    private void addToQueue(Message message) {

        if (message.getPriority().toString().equalsIgnoreCase(Priority.DUSUK.toString())) {        
            lowPriorityQueue.add(message);
        }
        else if (message.getPriority().toString().equalsIgnoreCase(Priority.NORMAL.toString())) {
            normalPriorityQueue.add(message);
        } 
        else if (message.getPriority().toString().equalsIgnoreCase(Priority.YUKSEK.toString()))  {
            highPriorityQueue.add(message);
        }

    }
    
    /**
     * Her bir nesneyi dosyayı yazma işleminde tekrar tekrar {@link ObjectOutputStream}
     * oluşturmamak için yazılan fonksiyon.
     * 
     * @throws IOException 
     */
    private void createOutputStreams() throws IOException {
        lowStream = new ObjectOutputStream(new FileOutputStream("low.ser"));
        normalStream = new ObjectOutputStream(new FileOutputStream("normal.ser"));
        highStream = new ObjectOutputStream(new FileOutputStream("high.ser"));
    }
        
    /**
     * Kullanılan reader ve writer kaynaklarını oluşturan fonksiyon.
     * 
     * @throws IOException 
     */
    private void createReaderAndWriter() throws IOException {
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pr = new PrintWriter(socket.getOutputStream());
    }

    /**
     * İstemci tarafından gönderilen mesaj bilgileri okunarak ilgili mesaj
     * önceliğine göre dosyalama sistemine ve veritabanına kaydedilir.
     * To ve Subject niteliklerinin 'stop' olması durumunda hata mesajı
     * gönderilir, öncelik değerinin uygun olmaması veya işlemin başarılı
     * olması durumunda istemciye ilgili mesaj gönderilir. Alınan mesajın
     * konusu sunucu tarafında gösterilir. Veritabanı işlemi sonucu oluşacak
     * herhangi bir hata loglanır.
     * 
     * @return {@link Message}
     */
    private Message takeClientMessage() throws IOException, NullPointerException{
        
        String to = br.readLine();
        String cc = br.readLine();
        String subject = br.readLine();
        String priority = br.readLine();
        
        Message incomingMessage = new Message();
        
        incomingMessage.setTo(to);
        incomingMessage.setCc(cc);
        incomingMessage.setSubject(subject);

        if (priority.equalsIgnoreCase(Priority.DUSUK.toString())) {
            incomingMessage.setPriority(Priority.DUSUK);
            //lowPriorityMessages.add(incomingMessage);
        }
        else if (priority.equalsIgnoreCase(Priority.NORMAL.toString())) {
            incomingMessage.setPriority(Priority.NORMAL);
            //normalPriorityMessages.add(incomingMessage);
        }
        else if (priority.equalsIgnoreCase(Priority.YUKSEK.toString())){
            incomingMessage.setPriority(Priority.YUKSEK);
            //highPriorityMessages.add(incomingMessage);
        }  
 
        return incomingMessage;
    }
    
    /**
     * İlgili thread sınıfının execute komutuyla çalıştığı ve sınıfın istemciye
     * hizmet vererek gerekli işlemlerin gerçekleştiği fonksiyon.
     */
    @Override
    public void run(){
        try {
            createReaderAndWriter();
        } 
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can not create streams", e);
            return;
        }
        try {
            createOutputStreams();
        } 
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can not create files", e);
            return;
        }
        Message clientMessage;

        while (socket.isConnected()) {    
            try {
                clientMessage = takeClientMessage();
                addToQueue(clientMessage);
            } 
            catch (IOException | NullPointerException e) {
                LOGGER.log(Level.SEVERE, "Client stopped the connection", e); 
                break;
            }
               
            if (clientMessage != null) {
                
                if (clientMessage.getTo().equalsIgnoreCase(Message.FLAG) ||
                        clientMessage.getSubject().equalsIgnoreCase(Message.FLAG)) {
                    break;
                }
                
                try {
                    writeMessageToFile(clientMessage);
                } 
                catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Problem while writing message to file", e);  
                    return;
                }
                /*try {
                    insertMessageToDatabase(clientMessage);
                }  
                catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Sql error occurs", e);  
                    return;
                }*/
                responseToClient(clientMessage);
            }
           
        }
        
        if (lowStream != null) {
            try {
                lowStream.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error closing stream", ex);
            }
        }
        
        if (normalStream != null) {
            try {
                lowStream.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error closing stream", ex);
            }
        }
        
        if (highStream != null) {
            try {
                lowStream.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error closing stream", ex);
            }
        }
        
        if (br != null) {
            try {
                br.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error closing buffer reader", ex);
            }
        }
        
        if(pr != null) {
            pr.close();
        }
            
    }
}
    
