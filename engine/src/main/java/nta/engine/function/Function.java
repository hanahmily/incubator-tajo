package nta.engine.function;

import nta.catalog.Column;
import nta.catalog.proto.CatalogProtos.DataType;
import nta.datum.Datum;
import nta.engine.exec.eval.EvalNode;

import com.google.gson.annotations.Expose;


public abstract class Function {
	@Expose
	protected String signature;
	@Expose
  protected Column [] definedArgs;
	@Expose
	protected EvalNode [] givenArgs;
	
	public Function(Column [] definedArgs) {
		this.definedArgs = definedArgs;
	}
	
	public final Column [] getDefinedArgs() {
		return this.definedArgs;
	}
	
	public void setSignature(String signature) {
	  this.signature = signature;
	}
	
	public String getSignature() {
	  return signature;
	}
	
	public abstract Datum invoke(Datum ... datums);
	public abstract DataType getResType();
	
	public enum Type {
	  AGG,
	  GENERAL;
	}
}
