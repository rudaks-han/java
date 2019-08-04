package kr.co.spectra.attic.tools.convert.customize;

import kr.co.spectra.attic.tools.convert.customize.share.Util;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class WordExtractor {

    List<String> tableTitle = new ArrayList<String>();
    Map<String, String> tableHeaderToKey;

    public WordExtractor(List<ConfigProperty.ExcelColumn> excelColumns) {
        this.tableHeaderToKey = mapTableHeaderToKey(excelColumns);
    }

    public List<HashMap<String, String>> parseWordDocument(String wordFilePath) {
        List<HashMap<String, String>> dataList = null;

        try {
            Util.printLog("parsing word file : " + wordFilePath);

            dataList = parse(wordFilePath);
            dataList = mapTableTitle(dataList);
            dataList = removeInList(dataList, "문서정보", "변경정보");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dataList;
    }

    private List<HashMap<String, String>> parse(String filepath) throws IOException {
        FileInputStream fis = null;
        List<HashMap<String, String>> dataList = null;

        try {
            fis = new FileInputStream(filepath);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            Iterator<IBodyElement> iter = xdoc.getBodyElementsIterator();

            String paragraph = null;

            while (iter.hasNext()) {

                IBodyElement elem = iter.next();

                if (elem instanceof XWPFParagraph) {
                    String text = parseWordParagraph(elem);

                    if ("".equals(text) || text == null)
                        continue;

                    paragraph = text;
                }

                else if (elem instanceof XWPFTable) {
                    tableTitle.add(paragraph);
                    dataList = parseWordTable(elem);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            fis.close();
        }

        return dataList;
    }

    private List<HashMap<String, String>> parseWordTable(IBodyElement elem) {
        List<HashMap<String, String>> dataList = new ArrayList<HashMap<String, String>>();

        List<XWPFTable> tableList = elem.getBody().getTables();
        for (XWPFTable table : tableList) {
            HashMap<String, String> map = new HashMap<String, String>();
            String previousHeaderText = null;

            for (int i = 0; i < table.getRows().size(); i++) {

                for (int j = 0; j < table.getRow(i).getTableCells().size(); j++) {

                    XWPFTableCell cell = table.getRow(i).getCell(j);
                    //String text = cell.getText();

                    if (j % 2 == 1) { // 테이블의 셀이 홀수번째일째 header로 저장한다.
                        String headerText = table.getRow(i).getCell(j-1).getText();

                        String key = null;

                        if ("".equals(headerText)) {
                            key = previousHeaderText;

                        } else {
                            key = tableHeaderToKey.get(headerText);
                        }

                        String text = appendMultiLineParagraphText(cell);


                        if (headerText != null && !"".equals(headerText))
                            previousHeaderText = key;

                        if (key != null) {

                            String value = map.get(key);
                            if (value != null) {
                                map.put(key, value + "\n" + text);
                            } else {
                                map.put(key, text);
                            }
                        }
                    }
                }
            }

            dataList.add(map);

        }

        return dataList;
    }

    private String parseWordParagraph(IBodyElement elem) {
        return ((XWPFParagraph) elem).getText();
    }

    private String appendMultiLineParagraphText(XWPFTableCell cell) {
        String text = "";
        for (int i=0; i<cell.getBodyElements().size(); i++) {
            IBodyElement element = cell.getBodyElements().get(i);

            if (element instanceof XWPFParagraph) {
                String temp = ((XWPFParagraph) element).getText();

                text += "\n" + temp ;
            }
        }

        return text;
    }

    private List<HashMap<String, String>> mapTableTitle(List<HashMap<String, String>> dataList) {
        if (dataList != null && dataList.size() > 0) {
            for (int i = 0; i < dataList.size(); i++) {
                HashMap<String, String> map = dataList.get(i);
                map.put("customizeName", tableTitle.get(i));
            }
        }

        return dataList;
    }

    private List<HashMap<String, String>> removeInList(List<HashMap<String, String>> dataList, String ... filterString) {
        for (int i=0; i<filterString.length; i++) {
            if (dataList != null && dataList.size() > 0) {
                for (int j = 0; j < dataList.size(); j++) {
                    HashMap<String, String> map = dataList.get(j);
                    if (map.get("customizeName").indexOf(filterString[i]) > -1) {
                        dataList.remove(j);
                    }
                }
            }
        }

        return dataList;
    }

    private Map<String, String> mapTableHeaderToKey(List<ConfigProperty.ExcelColumn> excelColumns) {

        Map<String, String> map = new HashMap<String, String>();

        for (ConfigProperty.ExcelColumn excelColumn: excelColumns) {
            String id = excelColumn.getId();
            String mapToWordParagraph = excelColumn.getMapToWordParagraph();

            if (mapToWordParagraph != null) {
                String[] mapToWordParagraphs = mapToWordParagraph.split(",");

                for (String temp : mapToWordParagraphs) {
                    map.put(temp, id);
                }
            }
        }

        return map;
    }
}
