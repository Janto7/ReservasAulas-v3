package org.iesalandalus.programacion.reservasaulas.mvc.modelo.negocio.ficheros;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.iesalandalus.programacion.reservasaulas.mvc.modelo.dominio.Profesor;
import org.iesalandalus.programacion.reservasaulas.mvc.modelo.negocio.IProfesores;

public class Profesores implements IProfesores {

	private static final String NOMBRE_FICHERO_PROFESORES = "datos/profesores.dat";
	// -----> coleccionProfesores (0...*)
	private List<Profesor> coleccionProfesores;

	public Profesores() {
		coleccionProfesores = new ArrayList<>();
	}

	public Profesores(IProfesores profesores) {
		setProfesores(profesores);
	}

	@Override
	public void comenzar() {
		leer();
	}

	private void leer() {
		File ficheroProfesores = new File(NOMBRE_FICHERO_PROFESORES);
		// Indicamos que se trata de un flujo de entrada para leer objetos
		try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(ficheroProfesores))) {
			Profesor profesor = null;
			do {
				// Casting
				profesor = (Profesor) entrada.readObject();
				insertar(profesor);
			} while (profesor != null);
			// Cerramos el flujo
			entrada.close();
		} catch (ClassNotFoundException e) {
			System.out.println("No puedo encontrar la clase que tengo que leer.");
		} catch (FileNotFoundException e) {
			System.out.println("No puedo abrir el fihero de profesores.");
		} catch (EOFException e) {
			System.out.println("Fichero profesores leído satisfactoriamente.");
		} catch (IOException e) {
			System.out.println("Error inesperado de Entrada/Salida.");
		} catch (OperationNotSupportedException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void terminar() {
		escribir();
	}

	private void escribir() {
		File ficheroAulas = new File(NOMBRE_FICHERO_PROFESORES);
		// Indicamos que se trata de un flujo de salida para escribir objetos
		try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(ficheroAulas))) {
			for (Profesor profesor : coleccionProfesores) {
				salida.writeObject(profesor);
			}
			System.out.println("Fichero profesores escrito satisfactoriamente.");
			// Cerramos el flujo
			salida.close();
		} catch (FileNotFoundException e) {
			System.out.println("No puedo crear el fichero de profesores.");
		} catch (IOException e) {
			System.out.println("Error inesperado de Entrada/Salida.");
		}
	}

	private void setProfesores(IProfesores profesores) {
		if (profesores == null) {
			throw new NullPointerException("ERROR: No se pueden copiar profesores nulos.");
		}
		coleccionProfesores = copiaProfundaProfesores(profesores.getProfesores());
	}

	// Comparamos las profesores con el método getCorreo() y los retornamos
	// ordenados.
	@Override
	public List<Profesor> getProfesores() {
		List<Profesor> profesoresOrdenados = copiaProfundaProfesores(coleccionProfesores);
		profesoresOrdenados.sort(Comparator.comparing(Profesor::getCorreo));
		return profesoresOrdenados;
	}

	private List<Profesor> copiaProfundaProfesores(List<Profesor> coleccionProfesores) {

		List<Profesor> otrosProfesores = new ArrayList<>();
		// Recorremos mientras queden elementos
		for (Iterator<Profesor> it = coleccionProfesores.iterator(); it.hasNext();) {

			Profesor profesor = it.next();
			otrosProfesores.add(new Profesor(profesor));

		}

		return otrosProfesores;

	}

	@Override
	public int getNumProfesores() {
		return coleccionProfesores.size();
	}

	@Override
	public void insertar(Profesor profesor) throws OperationNotSupportedException {
		if (profesor == null) {
			throw new NullPointerException("ERROR: No se puede insertar un profesor nulo.");
		}
		// Si la colección no contiene el profesor a introducir
		if (!coleccionProfesores.contains(profesor)) {
			// lo insertamos
			coleccionProfesores.add(new Profesor(profesor));
		} else {
			throw new OperationNotSupportedException("ERROR: Ya existe un profesor con ese correo.");
		}

	}

	@Override
	public Profesor buscar(Profesor profesor) {
		if (profesor == null) {
			throw new NullPointerException("ERROR: No se puede buscar un profesor nulo.");
		}
		/*
		 * Devuelve el índice de la primera aparición del elemento especificado en esta
		 * lista.
		 */
		int indice = coleccionProfesores.indexOf(profesor);
		// Devuelve -1 si el elemento especificado no está presente en esta lista.
		if (indice == -1) {
			return null;
		} else {
			return new Profesor(coleccionProfesores.get(indice));
		}
	}

	@Override
	public void borrar(Profesor profesor) throws OperationNotSupportedException {
		if (profesor == null) {
			throw new NullPointerException("ERROR: No se puede borrar un profesor nulo.");
		}
		int indice = coleccionProfesores.indexOf(profesor);
		if (indice == -1) {
			throw new OperationNotSupportedException("ERROR: No existe ningún profesor con ese correo.");
		} else {
			coleccionProfesores.remove(indice);
		}
	}

	@Override
	public List<String> representar() {
		List<String> representacion = new ArrayList<>();

		// Uso el método getProfesores() me devuelve la lista ordenada.
		for (Iterator<Profesor> it = getProfesores().iterator(); it.hasNext();) {

			representacion.add(it.next().toString());
		}
		return representacion;
	}

}
