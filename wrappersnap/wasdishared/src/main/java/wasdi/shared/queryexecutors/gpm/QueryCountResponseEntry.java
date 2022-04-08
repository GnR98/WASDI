package wasdi.shared.queryexecutors.gpm;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QueryCountResponseEntry {

	private String name;
	private String duration;
	private String accumulation;
	private String extension;
	private Date date;

}
