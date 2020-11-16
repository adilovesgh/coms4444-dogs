package dogs.sim;

import java.io.Serializable;

public class ParkLocation implements Serializable {

	private static double dataError = 1E-7;
	private static final long serialVersionUID = 1L;
	public static final Integer PARK_SIZE = 200;
	private Double row, column;
	
	public ParkLocation() {
		setRow(0.0);
		setColumn(0.0);
	}
	
	public ParkLocation(Double row, Double column) {
		setRow(row);
		setColumn(column);
	}
	
	public Double getRow() {
		return row;
	}
	
	public Double getColumn() {
		return column;
	}
	
	public void setRow(Double row) {
		this.row = row < 0 ? 0 : row > PARK_SIZE - 1 + dataError ? PARK_SIZE - 1 : row;
	}

	public void setColumn(Double column) {
		this.column = column < 0 ? 0 : column > PARK_SIZE - 1 + dataError ? PARK_SIZE - 1 : column;
	}
	
	@Override
	public String toString() {
		return "(" + row + ", " + column + ")";
	}
	
	public boolean equals(ParkLocation parkLocation) {
		return row.equals(parkLocation.getRow()) && column == parkLocation.getColumn();
	}	
}