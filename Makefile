CLASSES = Observation.class ColoredGrid.class HeatMap5.class GeneralScan3.class HeatMap.class
JAVAFLAGS = -J-Xmx48m

all: $(CLASSES)

%.class : %.java
	javac $(JAVAFLAGS) $<

clean:
	@rm -f *.class
