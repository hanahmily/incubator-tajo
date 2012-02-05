/**
 * 
 */
package nta.distexec;

import nta.engine.json.GsonCreator;

import com.google.gson.annotations.Expose;

/**
 * @author jihoon
 *
 */
public class DistPlan {

  @Expose
	private String planName;	// algorithm name
  @Expose
	private int outputNum;		// # of copies of output
	
	public void setPlanName(String name) {
		this.planName = name;
	}
	
	public void setOutputNum(int num) {
		this.outputNum = num;
	}
	
	public String getPlanName() {
		return this.planName;
	}
	
	public int getOutputNum() {
		return this.outputNum;
	}
	
	public String toJSON() {
	  return GsonCreator.getInstance().toJson(this, DistPlan.class);
	}
}
