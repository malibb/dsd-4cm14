public class EjerciciosSerie5 {

    public static void main(String[] args){
	int n = 0;
	n = Integer.parseInt(args[0]);
	byte[] cadenota = new byte[n*4];
	for(int i = 0; i<n;i++){
		 
	}
	String a = (String)generate3RandomMayus();
	System.out.println(a.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

	private static char[] generate3RandomMayus() {
		String a = '';
		for(int i = 0;i<3;i++) {
			threeMayus[i] = (char)(Math.random()*(91-65+1)+65);
		}
		return threeMayus;
	}
}

