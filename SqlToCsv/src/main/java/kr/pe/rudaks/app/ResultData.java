package kr.pe.rudaks.app;

import kr.pe.rudaks.app.util.RecordSet;

import java.util.List;

public class ResultData
{
	private String tabName;
	private RecordSet rset;

	public ResultData(String tabName, RecordSet rset) {
		this.tabName = tabName;
		this.rset = rset;
	}

	public String getTabName() {
		return tabName;
	}

	public void setTabName(String tabName) {
		this.tabName = tabName;
	}

	public RecordSet getRset() {
		return rset;
	}

	public void setRset(RecordSet rset) {
		this.rset = rset;
	}
}
