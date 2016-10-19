(function () {
    var items = [];

    String.prototype.toDOM = function () {
        var d = document,
            i, a = d.createElement("div"),
            b = d.createDocumentFragment();
        a.innerHTML = this;
        while (i = a.firstChild) b.appendChild(i);
        return b;
    };

    function render() {
        var vFilter = document.getElementById("rechercher").value;
        var itemsFiltered = vFilter ? items.filter(function (item) {
            item.body = item.body ||  '';
            return (item.title.toLowerCase().indexOf(vFilter.toLowerCase()) !== -1) ||
                (item.body.toLowerCase().indexOf(vFilter.toLowerCase()) !== -1)
        }) : items;

        document.getElementById("items").innerHTML = '';
        if (itemsFiltered.length === 0) {
            var el = `
<div class="empty">Aucun résultat</div>
`;
            document.getElementById("items").appendChild(el.toDOM());
        }

        for (var index in itemsFiltered) {
            var item = itemsFiltered[index];
            var ext = item.tags && item.tags.length ? item.tags[0] : '';
            var cls = '',
                style = '';
            if (item.download === -1) {
                cls = 'error';
            } else if (item.download === 1) {
                cls = 'complete';
            } else {
                style = 'background: linear-gradient(to right, #5bc0de ' + (item.download * 100) + '%, white ' + (100 - (item.download * 100)) + '%); */';
            }
            var el = `
<article class="card ${cls}" >
  <h3>${item.title} <span class="label">${ext}</span></h3>
        <div class="bar" style="${style}"></div>
</article>
`;
            document.getElementById("items").appendChild(el.toDOM());
        }
    }

    document.getElementById('rechercher').addEventListener('keyup', render);


    fetch('/data/files')
        .then(function (response) {
            if (response.status !== 200) {
                throw "oups";
            } else {
                return response.json();
            }
        })
        .then(function (data) {
            items = data;
            render();
        })
        .catch(function (e) {
            debugger;
            alert('Erreur lors de la récupération des fichiers');
        });
}());