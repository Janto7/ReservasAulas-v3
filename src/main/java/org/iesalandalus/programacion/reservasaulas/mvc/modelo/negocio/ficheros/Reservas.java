package org.iesalandalus.programacion.reservasaulas.mvc.modelo.negocio.ficheros;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.iesalandalus.programacion.reservasaulas.mvc.modelo.dominio.Aula;
import org.iesalandalus.programacion.reservasaulas.mvc.modelo.dominio.Permanencia;
import org.iesalandalus.programacion.reservasaulas.mvc.modelo.dominio.PermanenciaPorHora;
import org.iesalandalus.programacion.reservasaulas.mvc.modelo.dominio.PermanenciaPorTramo;
import org.iesalandalus.programacion.reservasaulas.mvc.modelo.dominio.Profesor;
import org.iesalandalus.programacion.reservasaulas.mvc.modelo.dominio.Reserva;
import org.iesalandalus.programacion.reservasaulas.mvc.modelo.negocio.IReservas;

public class Reservas implements IReservas {

	private static final float MAX_PUNTOS_PROFESOR_MES = 200.0f;
	private static final String NOMBRE_FICHERO_RESERVAS = "datos/reservas.dat";

	// -----> coleccionReservas (0...*)
	private List<Reserva> coleccionReservas;

	public Reservas() {
		coleccionReservas = new ArrayList<>();
	}

	public Reservas(IReservas reservas) {
		setReservas(reservas);
	}

	@Override
	public void comenzar() {
		leer();
	}

	private void leer() {
		File ficheroAulas = new File(NOMBRE_FICHERO_RESERVAS);
		// Indicamos que se trata de un flujo de entrada para leer objetos
		try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(ficheroAulas))) {
			Reserva reserva = null;
			do {
				// Casting
				reserva = (Reserva) entrada.readObject();
				insertar(reserva);
			} while (reserva != null);
			// Cerramos el flujo
			entrada.close();
		} catch (ClassNotFoundException e) {
			System.out.println("No puedo encontrar la clase que tengo que leer.");
		} catch (FileNotFoundException e) {
			System.out.println("No puedo abrir el fihero de reservas.");
		} catch (EOFException e) {
			System.out.println("Fichero reservas leído satisfactoriamente.");
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
		File ficheroAulas = new File(NOMBRE_FICHERO_RESERVAS);
		// Indicamos que se trata de un flujo de salida para escribir objetos
		try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(ficheroAulas))) {
			for (Reserva reserva : coleccionReservas) {
				salida.writeObject(reserva);
			}
			System.out.println("Fichero reservas escrito satisfactoriamente.");
			// Cerramos el flujo
			salida.close();
		} catch (FileNotFoundException e) {
			System.out.println("No puedo crear el fichero de reservas.");
		} catch (IOException e) {
			System.out.println("Error inesperado de Entrada/Salida.");
		}
	}

	private void setReservas(IReservas reservas) {
		if (reservas == null) {
			throw new NullPointerException("ERROR: No se pueden copiar reservas nulas.");
		}
		coleccionReservas = copiaProfundaReservas(reservas.getReservas());
	}

	private List<Reserva> copiaProfundaReservas(List<Reserva> reservas) {

		List<Reserva> otrasReservas = new ArrayList<>();
		// Recorremos mientras queden elementos
		for (Iterator<Reserva> it = reservas.iterator(); it.hasNext();) {

			Reserva reserva = it.next();
			otrasReservas.add(new Reserva(reserva));

		}

		return otrasReservas;

	}

	@Override
	public List<Reserva> getReservas() {
		List<Reserva> reservasOrdenadas = copiaProfundaReservas(coleccionReservas);

		/*
		 * Creamos un comparador de tipo aula. Donde comparamos las aulas con el método
		 * getNombre()
		 */
		Comparator<Aula> comparadorAula = Comparator.comparing(Aula::getNombre);
		/*
		 * Creamos un comparador de permanencia, valiéndonos de una expresión lambda,
		 * que es como un método: proporcionamos una lista de parámetros(p1,p2) y un
		 * cuerpo expresado en términos de esos parámetros.
		 * 
		 */
		Comparator<Permanencia> comparadorPermanencia = (Permanencia p1, Permanencia p2) -> {
			int comparacion = -1;

			// Si dos permanencias tienen el mismo día debemos comprobar si es una
			// permanencia por tramo o por hora
			if (p1.getDia().equals(p2.getDia())) {

				/*
				 * Si es una permanencia por tramo usamos el método getTramo(), nos devuelve un
				 * enumerado podemos comparar con el método ordinal()
				 */
				if (p1 instanceof PermanenciaPorTramo && p2 instanceof PermanenciaPorTramo) {
					comparacion = Integer.compare(((PermanenciaPorTramo) p1).getTramo().ordinal(),
							((PermanenciaPorTramo) p2).getTramo().ordinal());
					// Comparamos los enteros de la hora de cada permanencia
				} else if (p1 instanceof PermanenciaPorHora && p2 instanceof PermanenciaPorHora) {
					comparacion = ((PermanenciaPorHora) p1).getHora().compareTo(((PermanenciaPorHora) p2).getHora());
				}
			} else {
				// Si no tienen el mismo día nos basta con comparar los días.
				comparacion = p1.getDia().compareTo(p2.getDia());
			}
			return comparacion;
		};

		/*
		 * Comparamos primero las aulas para despues comparar la permencia de las aulas
		 * y las retornamos ordenadas.
		 */
		reservasOrdenadas.sort(Comparator.comparing(Reserva::getAula, comparadorAula)
				.thenComparing(Reserva::getPermanencia, comparadorPermanencia));
		return reservasOrdenadas;
	}

	@Override
	public int getNumReservas() {
		return coleccionReservas.size();
	}

	@Override
	public void insertar(Reserva reserva) throws OperationNotSupportedException {

		if (reserva == null) {
			throw new NullPointerException("ERROR: No se puede insertar una reserva nula.");
		}

		// Obtenemos la reserva del aula y día.
		Reserva reservaExistente = getReservaAulaDia(reserva.getAula(), reserva.getPermanencia().getDia());
		// Si existen reservas en ese aula y en ese día.
		if (reservaExistente != null) {

			// Comprobamos la reserva a insertar no sea del mismo tipo de permanencia
			if (reservaExistente.getPermanencia() instanceof PermanenciaPorTramo
					&& reserva.getPermanencia() instanceof PermanenciaPorHora) {
				throw new OperationNotSupportedException(
						"ERROR: Ya se ha realizado una reserva de otro tipo de permanencia para este día.");
			}
			if (reservaExistente.getPermanencia() instanceof PermanenciaPorHora
					&& reserva.getPermanencia() instanceof PermanenciaPorTramo) {
				throw new OperationNotSupportedException(
						"ERROR: Ya se ha realizado una reserva de otro tipo de permanencia para este día.");
			}
		}
		// Validamos antes de insertar la nueva reserva a la colección no sea del mismo
		// mes

		if (!esMesSiguienteOPosterior(reserva)) {
			throw new OperationNotSupportedException(
					"ERROR: Sólo se pueden hacer reservas para el mes que viene o posteriores.");
		}

		// Comprobamos no se hayan sobrepasado los puntos de la reserva
		if (getPuntosGastadosReserva(reserva) > MAX_PUNTOS_PROFESOR_MES) {
			throw new OperationNotSupportedException(
					"ERROR: Esta reserva excede los puntos máximos por mes para dicho profesor.");
		}
		// Comprobamos la reserva no exista
		if (coleccionReservas.contains(reserva)) {
			throw new OperationNotSupportedException("ERROR: Ya existe una reserva igual.");
		}
		// Si no existe la insertamos
		else {
			coleccionReservas.add(new Reserva(reserva));
		}

	}

	private boolean esMesSiguienteOPosterior(Reserva reserva) {

		// Obtenemos el día de la reserva
		LocalDate diaReserva = reserva.getPermanencia().getDia();
		// Obtenemos el més siguiente al actual con método plusMonths()
		LocalDate dentroDeUnMes = LocalDate.now().plusMonths(1);
		// Obtenemos el primer dia del més siguiente
		LocalDate primerDiaMesSiguiente = LocalDate.of(dentroDeUnMes.getYear(), dentroDeUnMes.getMonth(), 1);
		// Devuelve true si el dia de la reserva es despues del primer dia del mes
		// siguiente o igual.
		return diaReserva.isAfter(primerDiaMesSiguiente) || diaReserva.equals(primerDiaMesSiguiente);
	}

	/*
	 * Obtenemos las reservas de un profesor por més valiendonos del método
	 * GetReservasProfesorMes, acumulamos los puntos y los devolvemos.
	 */
	private float getPuntosGastadosReserva(Reserva reserva) {
		float puntosGastados = 0;
		for (Iterator<Reserva> it = getReservasProfesorMes(reserva.getProfesor(), reserva.getPermanencia().getDia())
				.iterator(); it.hasNext();) {
			Reserva reservaProfesor = it.next();
			puntosGastados += reservaProfesor.getPuntos();
		}
		return puntosGastados + reserva.getPuntos();
	}

	private List<Reserva> getReservasProfesorMes(Profesor profesor, LocalDate mes) {

		if (profesor == null) {
			throw new NullPointerException("No se pueden buscar reservas de un profesor nulo.");
		}

		List<Reserva> reservasMes = new ArrayList<>();
		// Recorremos todas las reservas
		for (Iterator<Reserva> it = getReservas().iterator(); it.hasNext();) {

			Reserva reserva = it.next();
			// Obtemos el día de la reserva
			LocalDate diaReserva = reserva.getPermanencia().getDia();
			/*
			 * Si la reserva es igual al profesor pasado por parámetro, y el día de la
			 * reserva tiene el mismo més que el mes pasado por parametro
			 */
			if (reserva.getProfesor().equals(profesor) && diaReserva.getMonthValue() == mes.getMonthValue()
					&& diaReserva.getYear() == mes.getYear()) {
				// La añadimos
				reservasMes.add(new Reserva(reserva));
			}

		}
		// Retornamos un arrayList con las reservas del profesor por més
		return reservasMes;

	}

	private Reserva getReservaAulaDia(Aula aula, LocalDate fechaDia) {

		if (fechaDia == null) {
			throw new NullPointerException("No se puede buscar reserva para un día nulo.");
		}

		Reserva reserva = null;
		for (Iterator<Reserva> it = coleccionReservas.iterator(); it.hasNext();) {

			reserva = it.next();

			if (reserva.getAula().equals(aula) && reserva.getPermanencia().getDia().equals(fechaDia)) {
				// La retornamos
				return reserva;

			}

		}
		return null;

	}

	@Override
	public Reserva buscar(Reserva reserva) {
		if (reserva == null) {
			throw new NullPointerException("ERROR: No se puede buscar una reserva nula.");
		}
		/*
		 * Devuelve el índice de la primera aparición del elemento especificado en esta
		 * lista.
		 */
		int indice = coleccionReservas.indexOf(reserva);
		// Devuelve -1 si el elemento especificado no está presente en esta lista.
		if (indice == -1) {
			return null;
		} else {
			return new Reserva(coleccionReservas.get(indice));
		}
	}

	@Override
	public void borrar(Reserva reserva) throws OperationNotSupportedException {
		/*
		 * El test está mal, me pide aqui un illegalArgumentException cuando deberia ser
		 * un NullPointerException
		 */

		if (reserva == null) {
			throw new IllegalArgumentException("ERROR: No se puede borrar una reserva nula.");
		}
		if (!esMesSiguienteOPosterior(reserva)) {
			throw new OperationNotSupportedException(
					"ERROR: Sólo se pueden anular reservas para el mes que viene o posteriores.");
		}

		int indice = coleccionReservas.indexOf(reserva);
		if (indice == -1) {
			throw new OperationNotSupportedException("ERROR: No existe ninguna reserva igual.");
		} else {
			coleccionReservas.remove(indice);
		}
	}

	@Override
	public List<String> representar() {
		List<String> representacion = new ArrayList<>();

		for (Iterator<Reserva> it = getReservas().iterator(); it.hasNext();) {

			representacion.add(it.next().toString());
		}
		return representacion;
	}

	@Override
	public List<Reserva> getReservasProfesor(Profesor profesor) {

		if (profesor == null) {
			throw new NullPointerException("ERROR: El profesor no puede ser nulo.");
		}

		List<Reserva> reservasProfesor = new ArrayList<>();
		// Recorremos todas las reservas
		for (Iterator<Reserva> it = getReservas().iterator(); it.hasNext();) {

			Reserva reserva = it.next();
			// Si el profesor de la reserva es igual que el profesor pasado por parámetro
			if (reserva.getProfesor().equals(profesor)) {
				// Las añadimos
				reservasProfesor.add(new Reserva(reserva));
			}

		}

		Comparator<Aula> comparadorAula = Comparator.comparing(Aula::getNombre);
		Comparator<Permanencia> comparadorPermanencia = (Permanencia p1, Permanencia p2) -> {
			int comparacion = -1;
			if (p1.getDia().equals(p2.getDia())) {
				if (p1 instanceof PermanenciaPorTramo && p2 instanceof PermanenciaPorTramo) {
					comparacion = Integer.compare(((PermanenciaPorTramo) p1).getTramo().ordinal(),
							((PermanenciaPorTramo) p2).getTramo().ordinal());
				} else if (p1 instanceof PermanenciaPorHora && p2 instanceof PermanenciaPorHora) {
					comparacion = ((PermanenciaPorHora) p1).getHora().compareTo(((PermanenciaPorHora) p2).getHora());
				}
			} else {
				comparacion = p1.getDia().compareTo(p2.getDia());
			}
			return comparacion;
		};
		reservasProfesor.sort(Comparator.comparing(Reserva::getAula, comparadorAula)
				.thenComparing(Reserva::getPermanencia, comparadorPermanencia));
		return reservasProfesor;
	}

	@Override
	public List<Reserva> getReservasAula(Aula aula) {
		if (aula == null) {
			throw new NullPointerException("ERROR: El aula no puede ser nula.");
		}
		List<Reserva> reservasAula = new ArrayList<>();

		// Recorremos todas las reservas
		for (Iterator<Reserva> it = getReservas().iterator(); it.hasNext();) {

			Reserva reserva = it.next();
			// Si el aula de la reserva es igual que el aula pasada por parámetro
			if (reserva.getAula().equals(aula)) {
				// La añadimos
				reservasAula.add(new Reserva(reserva));
			}

		}
		Comparator<Permanencia> comparadorPermanencia = (Permanencia p1, Permanencia p2) -> {
			int comparacion = -1;
			if (p1.getDia().equals(p2.getDia())) {
				if (p1 instanceof PermanenciaPorTramo && p2 instanceof PermanenciaPorTramo) {
					comparacion = Integer.compare(((PermanenciaPorTramo) p1).getTramo().ordinal(),
							((PermanenciaPorTramo) p2).getTramo().ordinal());
				} else if (p1 instanceof PermanenciaPorHora && p2 instanceof PermanenciaPorHora) {
					comparacion = ((PermanenciaPorHora) p1).getHora().compareTo(((PermanenciaPorHora) p2).getHora());
				}
			} else {
				comparacion = p1.getDia().compareTo(p2.getDia());
			}
			return comparacion;
		};
		reservasAula.sort(Comparator.comparing(Reserva::getPermanencia, comparadorPermanencia));
		return reservasAula;
	}

	@Override
	public List<Reserva> getReservasPermanencia(Permanencia permanencia) {

		if (permanencia == null) {
			throw new NullPointerException("ERROR: La permanencia no puede ser nula.");
		}

		List<Reserva> reservasPermanencia = new ArrayList<>();
		// Recorremos todas las reservas
		for (Iterator<Reserva> it = getReservas().iterator(); it.hasNext();) {

			Reserva reserva = it.next();
			// Si la permanencia de la reserva es igual que la permanencia pasada por
			// parámetro
			if (reserva.getPermanencia().equals(permanencia)) {

				reservasPermanencia.add(new Reserva(reserva));
			}

		}

		return reservasPermanencia;

	}

	@Override
	public boolean consultarDisponibilidad(Aula aula, Permanencia permanencia) {

		boolean disponible = true;

		if (aula == null) {
			throw new NullPointerException("ERROR: No se puede consultar la disponibilidad de un aula nula.");
		}

		if (permanencia == null) {
			throw new NullPointerException("ERROR: No se puede consultar la disponibilidad de una permanencia nula.");
		}

		for (Iterator<Reserva> it = coleccionReservas.iterator(); it.hasNext();) {

			Reserva reserva = it.next();

			if ((reserva.getPermanencia() instanceof PermanenciaPorTramo && permanencia instanceof PermanenciaPorHora
					|| reserva.getPermanencia() instanceof PermanenciaPorHora
							&& permanencia instanceof PermanenciaPorTramo)
					&& (reserva.getAula().equals(aula)
							&& reserva.getPermanencia().getDia().equals(permanencia.getDia()))) {
				disponible = false;
			}

			else if ((reserva.getPermanencia() instanceof PermanenciaPorTramo
					&& permanencia instanceof PermanenciaPorTramo
					|| reserva.getPermanencia() instanceof PermanenciaPorHora
							&& permanencia instanceof PermanenciaPorHora)
					&& (reserva.getAula().equals(aula) && reserva.getPermanencia().equals(permanencia))) {
				disponible = false;
			}

		}

		return disponible;
	}

}
