package chatapp;

import java.io.Serializable;

/**
* Sınıfa ait To, CC, Subject, Öncelik nitelikleri bulunur. 
* Soket haberleşmesinde bu sınıfa ait nesneler yaratılarak mesajlar aktarılır. 
* Nesne olarak dosyada tutulacağından Serializable implemente edildi.
* 
* @author batuhan özdöl
*/
public class Message implements Serializable {
    
    // Sınıf nitelikleri
    public static final String FLAG = "stop";
    public static final String LOW_PRIORITY = "low";
    public static final String NORMAL_PRIORITY = "normal";
    public static final String HIGH_PRIORITY = "high";
    
    private String to;
    private String subject;
    private String cc;
    private Priority pri;
    
    /**
    * Sınıf constructor
    */
    public Message() {     
    }
    
    // Setter metotları
    public void setSubject(String subject) {
        this.subject = subject;
    }
            
    public void setPriority(Priority pri) {
        this.pri = pri;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public void setTo(String to) {
        this.to = to;
    }

    // Getter metotları
    public Priority getPriority() {
        return pri;
    }

    public String getCc() {
        return cc;
    }

    public String getSubject() {
        return subject;
    }

    public String getTo() {
        return to;
    }
    
}
