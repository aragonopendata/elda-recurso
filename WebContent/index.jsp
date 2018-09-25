<%@ page language="java" contentType="text/html; charset=utf-8"  pageEncoding="utf-8"%>
<html lang="es">
 
<head>
<title>Estáticos</title>
<meta charset="utf-8" />
</head>
 
<body>
    <header>
       <h1>Estáticos</h1>       
    </header>
    <section>
       <article>           
  			<p><%= new java.util.Date().toString() %></p>
  			<p>ÁÉÍÓÚÑ</p>
  			<p>áéíóúñ</p>
       </article>
    </section>
    <footer>
        <p>Localidata</p>
    </footer>
</body>
</html>