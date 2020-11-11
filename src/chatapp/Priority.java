package chatapp;

/**
* Enum tipindeki Öncelik sınıfı paket içindeki diğer sınıfların 
* erişebilmesi için public tanımlandı. Önceki gözden geçirmede görünen 
* uyarı enum sınıfın non-public olması ve public bir metot içinde erişilebilir 
* bir şekilde tanımlanmamasından kaynaklanıyordu.
* 
* @author batuhan özdöl
*/   
public enum Priority {
    DUSUK, NORMAL, YUKSEK
}