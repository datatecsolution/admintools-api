package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**

* Esta clase astracta define a una persona juridica o natural del mundo real con metodos basicos

* @author: jdmayorga

* @version: 16/07/2015

* @see <a href = "https://www.github.com/jdmayorga" /> direccion del creador </a>

*/




@MappedSuperclass
public abstract  class Persona {
	@Column(name = "nombre")
	protected String nombre="";

	@Column(name = "apellido")
	protected String apellido="";

	@Column(name = "telefono")
	protected String telefono="";

	@Column(name = "correo")
	protected String correo="";

	@Column(name = "direccion")
	protected String direccion="";
	
	
	/**

     * metodo para establecer el nombre de la persona

     * @param n el nombre de la persona, n define el nombre de la persona 

     */
	
	public void setNombre(String n){
		nombre=n;
	}
	
	/**

     * M�todo que devuelve el nombre del la persona
     * @return el nombre de la persona

     */
	public String getNombre(){
		return nombre;
	}
	
	/**

     * metodo para establecer el apellido de la persona

     * @param a apellido de la persona, a define el apellido de la persona 

     */
	
	public void setApellido(String a){
		apellido=a;
	}
	
	/**

     * M�todo que devuelve el apellido del la persona
     * @return el apellido de la persona

     */
	public String getApellido(){
		return apellido;
	}
	
	/**

     * metodo para establecer el telefono de la persona

     * @param t telefono de la persona, t define el telefono de la persona 

     */
	
	public void setTelefono(String t){
		telefono=t;
	}
	
	/**

     * M�todo que devuelve el telefono del la persona
     * @return el telefono de la persona

     */
	public String getTelefono(){
		return telefono;
	}
	
	/**

     * metodo para establecer el nombre de la persona

     * @param c el correo de la persona, c define el correo de la persona 

     */
	
	public void setCorreo(String c){
		correo=c;
	}
	
	/**

     * M�todo que devuelve el correo electronico del la persona
     * @return el correo electronico de la persona

     */
	public String getCorreo(){
		return correo;
	}
	
	
	
	/**

     * metodo para establecer la direccion de la persona

     * @param d la direccion de la persona, d define la direccion de la persona 

     */
	public void setDireccion(String d){
		direccion=d;
	}
	
	/**

     * M�todo que devuelve la direccion del la persona
     * @return la direccion electronico de la persona

     */
	public String getDireccion(){
		return direccion;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Persona [nombre=" + nombre + ", apellido=" + apellido + ", telefono=" + telefono + ", correo=" + correo
				+ ", direccion=" + direccion + "]";
	}

}
