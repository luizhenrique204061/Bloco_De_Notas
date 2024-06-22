package DB

import Adapter.ListaNotasAdapter
import Adapter.NotasProtegidasAdapter
import Adapter.TarefasAdapterTelaPrincipal
import Modelo.Notas
import Modelo.NotasProtegidas
import Modelo.Tarefa
import Room.AppDataBase
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.olamundo.blocodenotas.MainActivity
import com.olamundo.blocodenotas.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DB {

    fun salvarNomeUsuario(nome: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        if (currentUser != null) {
            val usuarioId = currentUser.uid

            val nomeUsuario = hashMapOf(
                "nome" to nome
            )

            val documentoReferencia = db.collection("Usuarios").document(usuarioId)

            documentoReferencia.set(nomeUsuario)

                .addOnCompleteListener {
                    Log.i("DB", "Sucesso ao salvar os dados")
                }
                .addOnFailureListener { erro ->
                    Log.i("DB_Erro", "Erro ao salvar os dados! ${erro.printStackTrace()}")
                }
        }
    }

    fun atualizarNomeUsuario(nome: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (currentUser != null) {
            val usuarioId = currentUser.uid
            val documentoReferencia = db.collection("Usuarios").document(usuarioId)

            documentoReferencia.update("nome", nome)
                .addOnCompleteListener {
                    Log.i("DB", "Sucesso ao atualizar os dados")
                }
                .addOnFailureListener { erro ->
                    Log.i("DB_Erro", "Erro ao atualizar os dados! ${erro.printStackTrace()}")
                }
        } else {
            Log.i("DB_Erro", "Usuário não está logado")
        }
    }

    fun atualizarAnotacaoProtegida(anotacaoId: String, titulo: String, descricao: String, data: Long) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        currentUser?.let {
            val usuarioId = currentUser.uid
            val documentoReferencia = db.collection("Anotacoes_Usuario_Protegidas").document(usuarioId)
                .collection("Anotacoes_Protegidas").document(anotacaoId)

            documentoReferencia.update(
                mapOf(
                    "titulo" to titulo,
                    "descricao" to descricao,
                    "data" to data
                )
            )
                .addOnCompleteListener {
                    Log.i("db_anotacao", "Sucesso ao atualizar Anotação Protegida")
                }
                .addOnFailureListener { exception ->
                    Log.e("db_anotacao", "Falha ao atualizar Anotação Protegida", exception)
                }

        }
    }

    fun mostrarUsuarioActivityAlterarNome(nomeUsuario: TextView) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (currentUser != null) {
            val usuarioId = currentUser.uid

            val documentoReferencia = db.collection("Usuarios").document(usuarioId)

            documentoReferencia.addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    nomeUsuario.text = snapshot.getString("nome")
                }
            }
        }
    }

    fun recuperarNomeUsuario(nomeUsuario: String, textView: TextView) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        if (currentUser != null) {
            textView.visibility = View.VISIBLE
            val usuarioId = currentUser.uid

            val documentoReferencia = db.collection("Usuarios").document(usuarioId)

            documentoReferencia.addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val nome = snapshot.getString("nome")
                    if (nome != null) {
                        // Concatenar a string desejada com o nome do usuário recuperado
                        val context = textView.context
                        val mensagem = context.getString(R.string.ola_nome_do_usuario, nome)
                        textView.text = mensagem
                    }
                }
            }
        } else {
            textView.visibility = View.GONE
        }
    }

    fun recuperarNomeUsuarioEmailFragmento(nomeUsuario: TextView, nomeEmail: TextView) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (currentUser != null) {
            val usuarioId = currentUser.uid
            val email = currentUser.email

            val documentoReferencia = db.collection("Usuarios").document(usuarioId)
            documentoReferencia.addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    nomeUsuario.text = snapshot.getString("nome")
                    nomeEmail.text = email
                }
            }
        }
    }

    fun salvarAnotacoesProtegidas(titulo: String, descricao: String, data: Long) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val anotacaoId = UUID.randomUUID().toString()

        currentUser?.let {
            val usuarioId = currentUser.uid

            val notasProtegidasMap = hashMapOf(
                "titulo" to titulo,
                "descricao" to descricao,
                "data" to data,
                "usuarioId" to usuarioId,
                "anotacaoId" to anotacaoId
            )

            val documentoReferencia =
                db.collection("Anotacoes_Usuario_Protegidas").document(usuarioId)
                    .collection("Anotacoes_Protegidas").document(anotacaoId)

            documentoReferencia.set(notasProtegidasMap)
                .addOnCompleteListener {
                    Log.i("db_anotacao", "Sucesso ao salvar Anotação Protegida")
                }
                .addOnFailureListener {
                    Log.i("db_anotacao", "Falha ao salvar Anotação Protegida")
                }
        }
    }

    fun obterAnotacoesProtegidas(
        lista_anotacoes_protegidas: MutableList<NotasProtegidas>,
        adapter_Anotacoes_protegidas: NotasProtegidasAdapter,
        textViewAnotacoes: TextView,
        textViewNenhumaCorrespondencia: TextView
    ) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            val usuarioId = currentUser.uid

            val documentoReferencia =
                db.collection("Anotacoes_Usuario_Protegidas").document(usuarioId)
                    .collection("Anotacoes_Protegidas")

            documentoReferencia.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Erro ao obter dados: $error")
                }
                lista_anotacoes_protegidas.clear()

                for (documento in snapshot!!) {
                    val anotacoes = documento.toObject(NotasProtegidas::class.java)
                    lista_anotacoes_protegidas.add(anotacoes)
                }
                adapter_Anotacoes_protegidas.notifyDataSetChanged()

                if (lista_anotacoes_protegidas.isEmpty()) {
                    textViewAnotacoes.visibility = View.VISIBLE
                    textViewNenhumaCorrespondencia.visibility = View.GONE
                } else {
                    textViewAnotacoes.visibility = View.GONE
                    textViewNenhumaCorrespondencia.visibility = View.GONE
                }
            }

            /*
            documentoReferencia.get().addOnCompleteListener { tarefa ->
                if (tarefa.isSuccessful) {
                    for (documento in tarefa.result!!) {
                        val anotacoes = documento.toObject(NotasProtegidas::class.java)
                        lista_anotacoes_protegidas.add(anotacoes)
                        adapter_Anotacoes_protegidas.notifyDataSetChanged()
                    }
                    if (lista_anotacoes_protegidas.isEmpty()) {
                        textViewAnotacoes.visibility = View.VISIBLE
                    } else {
                        textViewAnotacoes.visibility = View.GONE
                    }
                }
            }

             */

        }
    }

    fun excluirAnotacoesProtegidas(anotacaoId: String) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            val usuarioId = currentUser.uid

            if (anotacaoId.isNotEmpty()) {
                val documentoReferencia = db.collection("Anotacoes_Usuario_Protegidas").document(usuarioId)
                    .collection("Anotacoes_Protegidas").document(anotacaoId)

                documentoReferencia.delete()
                    .addOnCompleteListener {
                        Log.i("db_anotacao", "Sucesso ao excluir Anotação Protegida com ID: $anotacaoId")
                    }
                    .addOnFailureListener { e ->
                        Log.i("db_anotacao", "Falha ao excluir Anotação Protegida com ID: $anotacaoId", e)
                    }
            }


        }
    }

    fun excluirTodasAsAnotacoesProtegidas() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        if (currentUser != null) {
            val usuarioId = currentUser.uid
            db.collection("Anotacoes_Usuario_Protegidas").document(usuarioId)
                .collection("Anotacoes_Protegidas")
                .get()
                .addOnCompleteListener { tarefa ->
                    if (tarefa.isSuccessful) {
                        for (anotacoes in tarefa.result!!) {
                            val anotacoesId = anotacoes.id
                            excluirAnotacoesProtegidas(anotacoesId)
                        }
                    }
                }
        }
    }

    fun salvarAnotacoesNaNuvem(id: Long, titulo: String, descricao: String, data: Long) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val usuarioId = currentUser.uid

            val notasMap = hashMapOf(
                "id" to id,
                "titulo" to titulo,
                "descricao" to descricao,
                "data" to data
            )

            val documentoReferencia = db.collection("Anotacoes_Usuario").document(usuarioId)
                .collection("Anotacoes").document(id.toString())

            documentoReferencia.set(notasMap)
                .addOnCompleteListener {
                    Log.i("Salvar", "Sucesso ao salvar Anotação")
                }
                .addOnFailureListener {
                    Log.i("Salvar", "Erro ao salvar Anotação")
                }
        } else {
            Log.e("AuthError", "Usuário não autenticado")
        }
    }

    fun salvarTarefasNaNuvem(id: Long, titulo: String, descricao: String, data: Long) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        currentUser?.let {
            val usuarioId = currentUser.uid

            val tarefasMap = hashMapOf(
                "id" to id,
                "titulo" to titulo,
                "descricao" to descricao,
                "data" to data
            )

            val documentoReferencia = db.collection("Tarefas_Usuario").document(usuarioId)
                .collection("Tarefas").document(id.toString())

            documentoReferencia.set(tarefasMap)

                .addOnCompleteListener {
                    Log.i("Salvar", "Sucesso ao salvar Tarefa")
                }
                .addOnFailureListener {
                    Log.i("Salvar", "Erro ao salvar Tarefa")
                }
        } ?: run {
            Log.e("AuthError", "Usuário não autenticado")
        }
    }

    suspend fun excluirAnotacoesUsuario(id: Long): Boolean {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        return if (currentUser != null) {
            val usuarioId = currentUser.uid
            val documentoReferencia = db.collection("Anotacoes_Usuario").document(usuarioId)
                .collection("Anotacoes").document(id.toString())

            try {
                documentoReferencia.delete().await()
                Log.i("Excluir", "Sucesso ao excluir Anotação")
                true
            } catch (e: Exception) {
                Log.e("Excluir", "Erro ao excluir Anotação", e)
                false
            }
        } else {
            Log.e("AuthError", "Usuário não autenticado")
            false
        }
    }

    suspend fun excluirTarefasUsuario(id: Long): Boolean {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        return if (currentUser != null) {
            val usuarioId = currentUser.uid
            val documentoReferencia = db.collection("Tarefas_Usuario").document(usuarioId)
                .collection("Tarefas").document(id.toString())

            try {
                documentoReferencia.delete().await()
                Log.i("Excluir", "Sucesso ao excluir Tarefa")
                true
            } catch (e: Exception) {
                Log.e("Excluir", "Erro ao excluir Tarefa", e)
                false
            }
        } else {
            Log.e("AuthError", "Usuário não autenticado")
            false
        }
    }

    fun excluirDadosDoUsaurio() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (currentUser != null) {
            val usuarioId = currentUser.uid

            val documentoReferencia = db.collection("Usuarios").document(usuarioId)

            documentoReferencia.delete()
                .addOnCompleteListener {
                    Log.i("DB", "Sucesso ao excluir dados do usuário")
                }.addOnFailureListener {
                    Log.i("DB", "Erro ao excluir dados do usuário")
                }
        }
    }

    fun realizarExclusao(context: Context) {
        val usuario = FirebaseAuth.getInstance().currentUser

        usuario?.let {
            excluirTodasAsAnotacoesDoUsuario()
            excluirTodasAsTarefasDoUsuario()
            excluirDadosDoUsaurio()
            excluirTodasAsAnotacoesProtegidas()


            usuario.delete()

                .addOnCompleteListener { tarefa ->
                    if (tarefa.isSuccessful) {
                        FirebaseAuth.getInstance().signOut()
                        Intent(context, MainActivity::class.java).apply {
                            context.startActivity(this)
                            Toast.makeText(
                                context,
                                context.getString(R.string.conta_excluida_com_sucesso),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
        }

    }

    fun excluirTodasAsAnotacoesDoUsuario() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        if (currentUser != null) {
            val usuarioId = currentUser.uid
            db.collection("Anotacoes_Usuario").document(usuarioId)
                .collection("Anotacoes")
                .get()
                .addOnCompleteListener { tarefa ->
                    if (tarefa.isSuccessful) {
                        for (anotacoes in tarefa.result!!) {
                            val anotacoesId = anotacoes.id
                            excluirAnotacoesDoUsuarioNaNuvem(usuarioId, anotacoesId)
                        }
                    }
                }
        }
    }

    fun excluirTodasAsTarefasDoUsuario() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        if (currentUser != null) {
            val usuarioId = currentUser.uid
            db.collection("Tarefas_Usuario").document(usuarioId)
                .collection("Tarefas")
                .get()
                .addOnCompleteListener { tarefa ->
                    if (tarefa.isSuccessful) {
                        for (tarefas in tarefa.result!!) {
                            val tarefasId = tarefas.id
                            excluirTarefasDoUsuarioNaNuvem(usuarioId, tarefasId)
                        }
                    }
                }
        }
    }

    fun excluirAnotacoesDoUsuarioNaNuvem(usuarioId: String, anotacaoId: String) {
        val db = FirebaseFirestore.getInstance()

        val documentoReferencia = db.collection("Anotacoes_Usuario").document(usuarioId)
            .collection("Anotacoes").document(anotacaoId)

        documentoReferencia.delete().addOnCompleteListener {
            Log.i("DB", "Sucesso ao excluir anotação do usuário")
        }.addOnFailureListener {
            Log.e("DB", "Erro ao excluir anotação do usuário")
        }
    }

    fun excluirTarefasDoUsuarioNaNuvem(usuarioId: String, tarefasId: String) {
        val db = FirebaseFirestore.getInstance()

        val documentoReferencia = db.collection("Tarefas_Usuario").document(usuarioId)
            .collection("Tarefas").document(tarefasId)

        documentoReferencia.delete().addOnCompleteListener {
            Log.i("DB", "Sucesso ao excluir tarefa do usuário")
        }.addOnFailureListener {
            Log.e("DB", "Erro ao excluir tarefa do usuário")
        }
    }

    fun obterAnotacoesDoUsuario(
        context: Context,
        lista_notas: MutableList<Notas>,
        adapter_notas: ListaNotasAdapter,
        textViewAnotacoes: TextView
    ) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val usuarioId = currentUser.uid

            db.collection("Anotacoes_Usuario").document(usuarioId).collection("Anotacoes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Erro ao obter dados: $error")
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        Log.w("Firestore", "Nenhuma anotação encontrada")
                        return@addSnapshotListener
                    }

                    // Lista temporária para armazenar novas anotações
                    val novasNotas = mutableListOf<Notas>()

                    // Limpa a lista atual de notas
                    lista_notas.clear()

                    // Itera sobre os documentos no snapshot
                    snapshot.forEach { documento ->
                        val nota = documento.toObject(Notas::class.java)

                        // Verifica se a nota já existe na lista de notas
                        val notaExistente = lista_notas.find { it.id == nota.id }

                        // Se a nota não existir na lista, adiciona à lista temporária de novas notas
                        if (notaExistente == null) {
                            novasNotas.add(nota)
                        }

                        // Adiciona a nota à lista de notas
                        lista_notas.add(nota)
                    }

                    // Notifica o adapter sobre as mudanças na lista de notas
                    adapter_notas.notifyDataSetChanged()

                    // Atualizar a visibilidade do "semAnotacoes"
                    if (lista_notas.isEmpty()) {
                        textViewAnotacoes.visibility = View.VISIBLE
                    } else {
                        textViewAnotacoes.visibility = View.GONE
                    }

                    // Se houver novas notas, salva-as no banco de dados local (Room)
                    if (novasNotas.isNotEmpty()) {
                        salvarNotasNoRoom(context, novasNotas)
                    }
                }
        } else {
            Log.e("AuthError", "Usuário não autenticado")
        }
    }

    fun obterTarefasDoUsuario(
        context: Context,
        lista_tarefas_tela_principal: MutableList<Tarefa>,
        adapter_tarefas_tela_principal: TarefasAdapterTelaPrincipal,
        textViewTarefas: TextView
    ) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val usuarioId = currentUser.uid

            db.collection("Tarefas_Usuario").document(usuarioId).collection("Tarefas")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Erro ao obter dados: $error")
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        Log.w("Firestore", "Nenhuma anotação encontrada")
                        return@addSnapshotListener
                    }

                    // Lista temporária para armazenar novas anotações
                    val novasTarefas = mutableListOf<Tarefa>()

                    // Limpa a lista atual de notas
                    lista_tarefas_tela_principal.clear()

                    // Itera sobre os documentos no snapshot
                    snapshot.forEach { documento ->
                        val tarefa = documento.toObject(Tarefa::class.java)

                        // Verifica se a nota já existe na lista de notas
                        val notaExistente = lista_tarefas_tela_principal.find { it.id == tarefa.id }

                        // Se a nota não existir na lista, adiciona à lista temporária de novas notas
                        if (notaExistente == null) {
                            novasTarefas.add(tarefa)
                        }

                        // Adiciona a nota à lista de tarefas
                        lista_tarefas_tela_principal.add(tarefa)
                    }

                    // Notifica o adapter sobre as mudanças na lista de notas
                    adapter_tarefas_tela_principal.notifyDataSetChanged()

                    // Atualizar a visibilidade do "semAnotacoes"
                    if (lista_tarefas_tela_principal.isEmpty()) {
                        textViewTarefas.visibility = View.VISIBLE
                    } else {
                        textViewTarefas.visibility = View.GONE
                    }

                    // Se houver novas notas, salva-as no banco de dados local (Room)
                    if (novasTarefas.isNotEmpty()) {
                        salvarTarefasNoRoom(context, novasTarefas)
                    }
                }
        } else {
            Log.e("AuthError", "Usuário não autenticado")
        }
    }

    fun salvarNotasNoRoom(context: Context, novasNotas: MutableList<Notas>) {
        val dao = AppDataBase.getInstance(context).NotaDao()

        // Executa em uma corrotina para operações no banco de dados
        CoroutineScope(Dispatchers.IO).launch {
            dao.inserirTodas(novasNotas)
        }
    }

    fun salvarTarefasNoRoom(context: Context, novasTarefas: MutableList<Tarefa>) {
        val dao = AppDataBase.getInstance(context).TarefaDao()

        // Executa em uma corrotina para operações no banco de dados
        CoroutineScope(Dispatchers.IO).launch {
            dao.inserirTodas(novasTarefas)
        }
    }

    fun buscarAnotacoesProtegidasPalavraChave(
        palavrachave: String,
        lista_anotacoes_tela_principal_protegida: MutableList<NotasProtegidas>,
        notas_protegidas_adapter: NotasProtegidasAdapter,
        textViewSemCorrespondencia: TextView
    ) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            val usuarioId = currentUser.uid
            val palavrachaveLowerCase = palavrachave.toLowerCase()

            db.collection("Anotacoes_Usuario_Protegidas").document(usuarioId).collection("Anotacoes_Protegidas")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Erro ao obter dados: $error")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        lista_anotacoes_tela_principal_protegida.clear()
                        for (documento in snapshot.documents) {
                            val anotacoesProtegidas = documento.toObject(NotasProtegidas::class.java)
                            anotacoesProtegidas?.let {
                                val termoDescricao = anotacoesProtegidas.titulo?.toLowerCase()
                                if (termoDescricao!!.contains(palavrachaveLowerCase)) {
                                    lista_anotacoes_tela_principal_protegida.add(anotacoesProtegidas)
                                }
                            }
                        }
                        notas_protegidas_adapter.notifyDataSetChanged()

                        if (lista_anotacoes_tela_principal_protegida.isNotEmpty()) {
                            textViewSemCorrespondencia.visibility = View.GONE
                            Log.d("FirestoreResponse", "Firestore encontrou resultados para a palavra-chave: $palavrachave")
                        } else {
                            textViewSemCorrespondencia.visibility = View.VISIBLE
                            Log.d("FirestoreResponse", "Firestore não encontrou resultados para a palavra-chave: $palavrachave")
                        }
                    } else {
                        textViewSemCorrespondencia.visibility = View.VISIBLE
                        Log.d("FirestoreResponse", "Nenhum dado encontrado no snapshot")
                    }
                }
        }
    }
}