package fr.bananasmoothii.scriptcommands.core.execution;

public class NoneType {
	public static final NoneType INSTANCE = new NoneType();
	
	@Override
	public boolean equals(Object o) {
		return o instanceof NoneType;
	}
	
	@Override
	public int hashCode() {
		return 1;
	}
}