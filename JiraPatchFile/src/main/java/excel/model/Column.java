package excel.model;

public class Column
{
    private String id;
    private String columnName;
    private boolean autoSize;
    private int addColumnWidth;

    public Column(String id, String columnName, boolean autoSize, int addColumnWidth)
    {
        this.id = id;
        this.columnName = columnName;
        this.autoSize = autoSize;
        this.addColumnWidth = addColumnWidth;
    }

    public String getId()
    {
        return id;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public boolean isAutoSize()
    {
        return autoSize;
    }

    public int getAddColumnWidth()
    {
        return addColumnWidth;
    }
}


