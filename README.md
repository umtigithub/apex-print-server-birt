<h1> apex-print-server-birt</h1>

<p>
  Este projeto fornece um <strong>Print Server baseado em Eclipse BIRT</strong> para geração de relatórios 
  (PDF, HTML, Excel etc.), sendo ideal para integração com aplicações web como 
  <strong>Oracle APEX</strong> ou qualquer aplicação que precise gerar relatórios a partir de templates
  <code>.rptdesign</code>.
</p>

<hr>

<h2>Tecnologias Utilizadas</h2>
<ul>
  <li>Java</li>
  <li>BIRT Runtime</li>
  <li>Maven</li>
  <li>Docker</li>
</ul>

<hr>

<h2>Como Usar / Configurar</h2>

<h3>1. Build local (sem Docker)</h3>
<pre><code>./mvnw clean package
</code></pre>

<h3>2. Executar via Docker</h3>
<pre><code>docker build -t apex-print-server-birt .
docker run -p 8080:8080 apex-print-server-birt
</code></pre>

<p>Se a porta 8080 estiver ocupada, altere para outra porta.</p>

<h3>3. Integração com Oracle APEX</h3>
<p>
  No APEX, configure o <em>Print Server</em> apontando para o host e porta onde este servidor 
  estiver rodando.  
  Basta enviar ao servidor o relatório <code>.rptdesign</code> e ele retornará o arquivo gerado 
  no formato desejado (PDF, HTML, XLS, etc.).
</p>

<hr>

<h2>O que o projeto já inclui</h2>
<ul>
  <li>Runtime completo do BIRT</li>
  <li>Dockerfile para containerização</li>
  <li>Scripts auxiliares de build e instalação</li>
  <li>Estrutura de projeto pronta para uso/expansão</li>
</ul>

<hr>

<h2> Como estender</h2>
<p>Você pode:</p>
<ul>
  <li>Adicionar novos relatórios <code>.rptdesign</code></li>
  <li>Configurar novas fontes de dados no BIRT</li>
  <li>Modificar o servidor para suportar autenticação</li>
  <li>Integrar com qualquer aplicação Java ou REST</li>
</ul>

<hr>

<h2> Quando usar este print server</h2>
<ul>
  <li>Quando você já usa BIRT para relatórios</li>
  <li>Quando precisa de uma engine de relatórios open-source e customizável</li>
  <li>Quando precisa gerar múltiplos formatos de saída</li>
</ul>

<hr>

<h2> Contribuições</h2>
<p>
  Contribuições são bem-vindas!  
  Você pode abrir issues, enviar pull requests ou sugerir melhorias.
</p>

<hr>

<h2> Licença</h2>
<p>
  Verifique as licenças do BIRT e demais dependências antes de uso em produção.
</p>

</body>
</html>
