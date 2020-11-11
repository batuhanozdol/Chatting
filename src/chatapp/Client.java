package chatapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Program argumanı olarak verilen host ve port bilgilerini kullanarak sunucuya
 * bağlanır ve komut satırı yoluyla kullanıcıdan aldığı veriyle {@link Message}
 * nesneleri oluşturur ve sunucuya gönderir.
 *
 * @author batuhan özdöl
 */
public class Client extends JFrame implements AutoCloseable, ActionListener {
   
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    
    private Socket socket;
    private PrintWriter pr;
    private BufferedReader br;   
    private JPanel panel;
    private JTextField toField, ccField, subjectField;
    private JComboBox priorityBox;
    private JButton sendButton;
    
    /**
    * İstemci sınıfı constructor
    */
    public Client() {  
    }
    
    private void createForm() {
        
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0,0,450,300);
        add(panel);
        
        JLabel l3 = new JLabel("To");
        l3.setBounds(110,15,100,20);
        panel.add(l3);
        
        toField = new JTextField();
        toField.setBounds(190, 15, 100, 20);
        panel.add(toField);
        
        JLabel l4 = new JLabel("Cc");
        l4.setBounds(110,35,100,20);
        panel.add(l4);
        
        ccField = new JTextField();
        ccField.setBounds(190, 35, 100, 20);
        panel.add(ccField);
        
        JLabel l5 = new JLabel("Subject");
        l5.setBounds(110,55,100,20);
        panel.add(l5);
        
        subjectField = new JTextField();
        subjectField.setBounds(190, 55, 100, 20);
        panel.add(subjectField);
        
        JLabel l6 = new JLabel("Priority");
        l6.setBounds(110,75,100,20);
        panel.add(l6);
        
        priorityBox = new JComboBox();
        priorityBox.addItem("low");
        priorityBox.addItem("normal");
        priorityBox.addItem("high");
        priorityBox.setBounds(190, 75, 100, 20);
        panel.add(priorityBox);
        
        sendButton = new JButton("Send");
        sendButton.setBounds(190, 105, 100, 30);      
        sendButton.addActionListener(this);
        panel.add(sendButton);
        
        setLayout(null);
        setSize(450,300);
        setLocation(500,75);
        setVisible(true);
        
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        
        String to = toField.getText();
        String cc = ccField.getText();
        String subject = subjectField.getText();
        String priority = priorityBox.getSelectedItem().toString();
        
        Message clientMessage = takeMessageInputs(to, cc, subject, priority);
    
        if (clientMessage != null) {
            sendMessage(clientMessage);
            readServerResponse();
        }
      
    }
    
    /**
     * Host ve port numarası kullanılarak sunucu tarafıyla soket bağlantısı 
     * kurulur, writer ve reader oluşturulur. Input output işlemleri için soket 
     * tarafında hata olması durumunda oluşan hata loglanır. Bağlantının 
     * başarısız olması durumunda exception gönderilir.
     * 
     * @param host
     * @param port 
     * @exception IOException
     */
    private void createSocket(String host, int port) throws IOException { 
        
        socket = new Socket(host, port);
        pr = new PrintWriter(socket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
    
    }
    
    /**
     * Mesaj alanı GUI tarafında JFrame ile alınarak {@link Message} nesnesi
     * oluşturulup bu nesneyi döndürür. Kullanıcı To, Subject ve Öncelik
     * niteliklerini uygun olarak doldurmak zorundadır; To veya Subject 
     * niteliklerinin STOP kelimesi olması durumunda socket bağlantısı kapatılır.
     * 
     * @param to
     * @param cc
     * @param subject
     * @param priority
     * @return {@link Message}
     */
    private Message takeMessageInputs(String to, String cc, String subject, String priority) {              

        if (to != null && subject != null) {
                    
            if (to.equalsIgnoreCase(Message.FLAG) || subject.equalsIgnoreCase(Message.FLAG)) {      
                socket = null;
                return null;
            } 
            else if (!priority.equalsIgnoreCase(Message.LOW_PRIORITY) && !priority.equalsIgnoreCase(Message.NORMAL_PRIORITY) 
                && !priority.equalsIgnoreCase(Message.HIGH_PRIORITY)) {                
                LOGGER.log(Level.SEVERE, "Wrong priority"); 
                return null;
            }
            else {
                Message message = new Message();
                message.setTo(to);
                message.setCc(cc);
                message.setSubject(subject);

                if(priority.equalsIgnoreCase(Message.LOW_PRIORITY)) {
                    message.setPriority(Priority.DUSUK);
                }
                else if(priority.equalsIgnoreCase(Message.LOW_PRIORITY)) {
                    message.setPriority(Priority.NORMAL);
                }
                else {
                    message.setPriority(Priority.YUKSEK);
                }

                return message;
            }
            
        }
        else {
            LOGGER.log(Level.SEVERE, "To and Cc must be given"); 
            return null;    
        }
                
    }        
    
    /**
     * {@link Message} nesnesinden alınan her bir nitelik sunucu
     * tarafına gönderilir.
     * 
     * @param message
     */
    private void sendMessage(Message message) {
        
        pr.println(message.getTo());      
        pr.println(message.getCc()); 
        pr.println(message.getSubject()); 
        pr.println(message.getPriority().toString());                
        pr.flush(); 
    
    }
    
    /**
     * Sunucudan alınan response mesajı okunarak komut satırına yazdırılır.
     */
    private void readServerResponse() {
        
        try {
            String serverMessage = br.readLine(); 
            System.out.println("\nServer says: " + serverMessage );
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while reading response from server", e);   
        }
        
    }
    
    /**
     * Bu fonksiyon istemci sınıfının yapacağı işlemlerin ilgili fonksiyonların 
     * çağrılarak yapılmasını sağlar. Bağlantı ve mesaj gönderme işlemleri
     * yapıldıktan sonra açık kalan kaynaklar kapatılır. Input veya output
     * işlemlerinde hata olması durumunda hata loglanır.
     * 
     * @param host
     * @param port
     */
    private void run(String host, int port) {          
        
        try {
            createSocket(host, port);
        } 
        catch(IOException e) {
            LOGGER.log(Level.SEVERE, "Can not connect to server", e);
            return;  
        }
        System.out.println("Client \n------");
        clientUI();
    }
    
    private void clientUI() {
        
        createForm();
        while (socket != null) {}
        panel.setVisible(false);
    
    }
    
    /**
     * Client sınıfının işini tamamlaması veya oluşan bir hatanın olması
     * durumunda bu fonksiyon çağrılarak kullanılan kaynaklar kapatılır.
     */
    @Override
    public void close() {
        
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error closing socket", ex);
            }
        }
        
        if (br != null) {
            try {
                br.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error closing buffer reader", ex);
            }
        }
        
        if (pr != null) {
            pr.close();
        }
        
    }
    
    /**
     * Ana fonksiyon, istemci sınıfı nesnesi oluşturarak komut satırından 
     * alınacak bilgilere göre sunucuyla bağlantı kurup istemci tarafını
     * çalıştırır. Komut satırından alınan argümanın integer sınıfına 
     * dönüştürülememsi durumunda hata loglanır.
     * 
     * @param args
     * @exception NumberFormatException
     */
    public static void main(String[] args) {                
        
        if (args.length != 2) {
            LOGGER.log(Level.SEVERE, "Exactly two parameters required");
            return;
        }
        
        try (Client client = new Client()) {
            String host = args[0];      
            int portNumber = Integer.parseInt(args[1]);
            client.run(host, portNumber);
        }
        catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid arguments", e);
        }
        
    }
   
}
